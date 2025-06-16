package com.energyauctions.french_auction_scraper.service;

import com.energyauctions.french_auction_scraper.model.Auction;
import com.energyauctions.french_auction_scraper.model.AuctionRegion;
import com.energyauctions.french_auction_scraper.model.AuctionTechnology;
import com.energyauctions.french_auction_scraper.repository.AuctionRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EEX Auction Data Scraper Service
 *
 * This service automatically collects French energy certificate auction data from the EEX website.
 * It runs daily at 2 am, extracts volume and pricing information from HTML tables,
 * and saves the data to our PostgreSQL database while preventing duplicates.
 *
 * Key Functions:
 * - Scheduled daily scraping at 02:00 (2 AM)
 * - Manual trigger capability for testing
 * - Extracts regional and technology auction data
 * - Handles European number formatting and currency parsing
 * - Validates data and prevents duplicate entries
 */

@Service
public class EEXAuctionScraperService {

    private static final Logger logger = LoggerFactory.getLogger(EEXAuctionScraperService.class);
    private static final String EEX_URL = "https://www.eex.com/en/markets/energy-certificates/french-auctions-power";

    @Autowired
    private AuctionRepository auctionRepository;

    // Run every day at 2 AM to check for new auction results
    @Scheduled(cron = "0 0 23 * * ?")  // 23:00 UTC = 2:00 AM EEST
    public void scrapeAuctionData() {
        logger.info("Starting EEX auction data scraping...");

        try {
            scrapeLatestAuctionResults();
            logger.info("EEX auction data scraping completed successfully");

        } catch (Exception e) {
            logger.error("Failed to scrape EEX auction data: {}", e.getMessage(), e);
        }
    }

    // Manual trigger for testing
    public void scrapeNow() {
        logger.info("Manual scraping triggered");
        scrapeAuctionData();
    }

    private void scrapeLatestAuctionResults() throws Exception {
        // Connect to EEX website with proper headers
        Document doc = Jsoup.connect(EEX_URL)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get();

        logger.info("Successfully connected to EEX website");

        // Find the Results section
        Element resultsSection = doc.selectFirst("div.col-xl-8.offset-xl-2:has(h2:contains(Results))");
        if (resultsSection == null) {
            logger.warn("Could not find Results section on EEX page");
            return;
        }

        logger.info("Found Results section");

        AuctionMetadata metadata = extractAuctionMetadata(resultsSection);
        if (metadata == null) {
            logger.warn("Could not extract auction metadata");
            return;
        }

        // Check if we already have this auction
        Optional<Auction> existingAuction = auctionRepository.findByAuctionDateAndProductionMonth(
                metadata.auctionDate, metadata.productionMonth);

        if (existingAuction.isPresent()) {
            logger.info("Auction for {} (production: {}) already exists, skipping",
                    metadata.auctionDate, metadata.productionMonth);
            return;
        }

        Auction auction = new Auction(metadata.auctionDate, metadata.productionMonth, metadata.reservePrice);

        // Extract regional data from the first table
        List<AuctionRegion> regions = extractRegionalData(resultsSection, auction);
        auction.setRegions(regions);

        // Extract technology data from the second table
        List<AuctionTechnology> technologies = extractTechnologyData(resultsSection, auction);
        auction.setTechnologies(technologies);

        // Save to database if we have data
        if (!regions.isEmpty() || !technologies.isEmpty()) {
            auctionRepository.save(auction);
            logger.info("Saved new auction: {} regions, {} technologies",
                    regions.size(), technologies.size());
        } else {
            logger.warn("No auction data found to save");
        }
    }

    private AuctionMetadata extractAuctionMetadata(Element resultsSection) {
        AuctionMetadata metadata = new AuctionMetadata();

        // Extract production month from table header "February 2025"
        Element monthHeader = resultsSection.selectFirst("th[colspan=4]");
        if (monthHeader != null) {
            metadata.productionMonth = monthHeader.text().trim();
            logger.info("Found production month: {}", metadata.productionMonth);
        } else {
            metadata.productionMonth = "Unknown";
            logger.warn("Could not find production month");
        }

        // Set auction date to current date (when results are published)
        metadata.auctionDate = LocalDate.now();

        // Extract reserve price from text like "The reserve price for the May auctions is: 0,15 €/MWh"
        String sectionText = resultsSection.text();
        Pattern pricePattern = Pattern.compile("reserve price.*?(\\d+[.,]\\d+).*?€/MWh", Pattern.CASE_INSENSITIVE);
        Matcher priceMatcher = pricePattern.matcher(sectionText);

        if (priceMatcher.find()) {
            String priceStr = priceMatcher.group(1).replace(",", ".");
            metadata.reservePrice = new BigDecimal(priceStr);
            logger.info("Found reserve price: {}", metadata.reservePrice);
        } else {
            metadata.reservePrice = BigDecimal.valueOf(0.15);
            logger.warn("Could not find reserve price, using default: 0.15");
        }

        logger.info("Extracted metadata: auction={}, production={}, reserve={}",
                metadata.auctionDate, metadata.productionMonth, metadata.reservePrice);

        return metadata;
    }

    private List<AuctionRegion> extractRegionalData(Element resultsSection, Auction auction) {
        List<AuctionRegion> regions = new ArrayList<>();

        // Find the first table (regional data) - it has headers with <p> tags
        Elements tables = resultsSection.select("table");
        Element regionalTable = null;

        for (Element table : tables) {
            // Check if this table has Region header with <p> tag
            if (table.select("p:contains(Region)").size() > 0) {
                regionalTable = table;
                break;
            }
        }

        if (regionalTable == null) {
            logger.warn("Could not find regional data table");
            return regions;
        }

        logger.info("Found regional data table");

        // Get all data rows (skip header rows)
        Elements dataRows = regionalTable.select("tr:has(td)");
        // Filter out header rows by checking if first cell contains "Region"
        dataRows = dataRows.select("tr:not(:has(p:contains(Region))):not(:has(td:contains(Volume Offered)))");

        logger.info("Found {} regional data rows", dataRows.size());

        for (Element row : dataRows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) continue;

            try {
                String regionName = extractCellText(cells.get(0));
                Integer volumeOffered = parseVolume(extractCellText(cells.get(1)));
                Integer volumeAllocated = parseVolume(extractCellText(cells.get(2)));
                BigDecimal avgPrice = parsePrice(extractCellText(cells.get(3)));

                if (volumeOffered != null && volumeAllocated != null && avgPrice != null && !regionName.isEmpty()) {
                    AuctionRegion region = new AuctionRegion(auction, regionName, volumeOffered, volumeAllocated, avgPrice);
                    regions.add(region);
                    logger.debug("Parsed region: {} - {} MWh at €{}/MWh", regionName, volumeAllocated, avgPrice);
                }

            } catch (Exception e) {
                logger.warn("Failed to parse regional row: {} - {}", row.text(), e.getMessage());
            }
        }

        return regions;
    }

    private List<AuctionTechnology> extractTechnologyData(Element resultsSection, Auction auction) {
        List<AuctionTechnology> technologies = new ArrayList<>();

        // Find the second table (technology data) - it has "Technology" header without <p> tags
        Elements tables = resultsSection.select("table");
        Element technologyTable = null;

        for (Element table : tables) {
            // Check if this table has Technology header directly in td (not in <p>)
            if (table.select("td:contains(Technology)").size() > 0 &&
                    table.select("p:contains(Technology)").size() == 0) {
                technologyTable = table;
                break;
            }
        }

        if (technologyTable == null) {
            logger.warn("Could not find technology data table");
            return technologies;
        }

        logger.info("Found technology data table");

        Elements dataRows = technologyTable.select("tr:has(td)");

        dataRows = dataRows.select("tr:not(:has(td:contains(Technology))):not(:has(td:contains(Volume Offered)))");

        logger.info("Found {} technology data rows", dataRows.size());

        for (Element row : dataRows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) continue;

            try {
                String technologyType = extractCellText(cells.get(0));
                Integer volumeOffered = parseVolume(extractCellText(cells.get(1)));
                Integer volumeAllocated = parseVolume(extractCellText(cells.get(2)));
                BigDecimal avgPrice = parsePrice(extractCellText(cells.get(3)));

                if (volumeOffered != null && volumeAllocated != null && avgPrice != null && !technologyType.isEmpty()) {
                    AuctionTechnology technology = new AuctionTechnology(auction, technologyType, volumeOffered, volumeAllocated, avgPrice);
                    technologies.add(technology);
                    logger.debug("Parsed technology: {} - {} MWh at €{}/MWh", technologyType, volumeAllocated, avgPrice);
                }

            } catch (Exception e) {
                logger.warn("Failed to parse technology row: {} - {}", row.text(), e.getMessage());
            }
        }

        return technologies;
    }

    private String extractCellText(Element cell) {
        // Try to get text from <p> tag first, fallback to direct text
        Element pTag = cell.selectFirst("p");
        String text = pTag != null ? pTag.text() : cell.text();
        return text.trim().replaceAll("\\s+", " ");
    }

    // Parse volume numbers like "236.995" or "1.943.184"
    private Integer parseVolume(String volumeText) {
        try {
            // Remove all non-digits except dots
            String cleaned = volumeText.replaceAll("[^0-9.]", "");
            // Remove dots (thousand separators) and convert to integer
            cleaned = cleaned.replace(".", "");
            return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
        } catch (Exception e) {
            logger.debug("Could not parse volume: {}", volumeText);
            return null;
        }
    }

    // Parse price values like "€ 0.50" or "€ 0.49"
    private BigDecimal parsePrice(String priceText) {
        try {
            // Extract number after € symbol
            Pattern pattern = Pattern.compile("€.*?(\\d+[.,]\\d+)");
            Matcher matcher = pattern.matcher(priceText);

            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", ".");
                return new BigDecimal(priceStr);
            }

            return null;
        } catch (Exception e) {
            logger.debug("Could not parse price: {}", priceText);
            return null;
        }
    }

    // Helper class to hold auction metadata
    private static class AuctionMetadata {
        LocalDate auctionDate;
        String productionMonth;
        BigDecimal reservePrice;
    }
}