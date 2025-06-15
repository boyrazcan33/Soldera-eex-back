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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EEXAuctionScraperService {

    private static final Logger logger = LoggerFactory.getLogger(EEXAuctionScraperService.class);
    private static final String EEX_URL = "https://www.eex.com/en/markets/energy-certificates/french-auctions-power";

    @Autowired
    private AuctionRepository auctionRepository;

    // Run every day at 3 PM to check for new auction results
    @Scheduled(cron = "0 0 15 * * ?")
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

        // Look for auction results section
        Element resultsSection = findResultsSection(doc);
        if (resultsSection == null) {
            logger.warn("Could not find auction results section on EEX page");
            return;
        }

        // Extract auction metadata (date and production month)
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

        // Create new auction record
        Auction auction = new Auction(metadata.auctionDate, metadata.productionMonth, metadata.reservePrice);

        // Extract regional data
        List<AuctionRegion> regions = extractRegionalData(resultsSection, auction);
        auction.setRegions(regions);

        // Extract technology data
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

    private Element findResultsSection(Document doc) {
        // Look for the results section - try multiple selectors
        Element results = doc.selectFirst("div:contains(Results)");
        if (results == null) {
            results = doc.selectFirst("section:contains(February 2025)");
        }
        if (results == null) {
            // Look for tables that might contain auction data
            Elements tables = doc.select("table");
            for (Element table : tables) {
                if (table.text().contains("Region") && table.text().contains("Volume") && table.text().contains("Price")) {
                    return table.parent();
                }
            }
        }
        return results;
    }

    private AuctionMetadata extractAuctionMetadata(Element resultsSection) {
        AuctionMetadata metadata = new AuctionMetadata();

        // Look for date patterns in the text
        String sectionText = resultsSection.text();

        // Extract month/year pattern like "February 2025"
        Pattern monthPattern = Pattern.compile("(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{4})");
        Matcher monthMatcher = monthPattern.matcher(sectionText);

        if (monthMatcher.find()) {
            metadata.productionMonth = monthMatcher.group(1) + " " + monthMatcher.group(2);
        } else {
            metadata.productionMonth = "Unknown";
        }

        // Set auction date to current date (when results are published)
        metadata.auctionDate = LocalDate.now();

        // Look for reserve price
        Pattern pricePattern = Pattern.compile("reserve price.*?(\\d+[.,]\\d+).*?€/MWh", Pattern.CASE_INSENSITIVE);
        Matcher priceMatcher = pricePattern.matcher(sectionText);

        if (priceMatcher.find()) {
            String priceStr = priceMatcher.group(1).replace(",", ".");
            metadata.reservePrice = new BigDecimal(priceStr);
        } else {
            metadata.reservePrice = BigDecimal.valueOf(0.15); // Default reserve price
        }

        logger.info("Extracted metadata: auction={}, production={}, reserve={}",
                metadata.auctionDate, metadata.productionMonth, metadata.reservePrice);

        return metadata;
    }

    private List<AuctionRegion> extractRegionalData(Element resultsSection, Auction auction) {
        List<AuctionRegion> regions = new ArrayList<>();

        // Look for the regional data table
        Element regionalTable = findTableByHeaders(resultsSection, "Region", "Volume");
        if (regionalTable == null) {
            logger.warn("Could not find regional data table");
            return regions;
        }

        Elements dataRows = regionalTable.select("tbody tr");
        logger.info("Found {} regional data rows", dataRows.size());

        for (Element row : dataRows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) continue;

            try {
                String regionName = cleanText(cells.get(0).text());
                Integer volumeOffered = parseVolume(cells.get(1).text());
                Integer volumeAllocated = parseVolume(cells.get(2).text());
                BigDecimal avgPrice = parsePrice(cells.get(3).text());

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

        // Look for the technology data table
        Element technologyTable = findTableByHeaders(resultsSection, "Technology", "Volume");
        if (technologyTable == null) {
            logger.warn("Could not find technology data table");
            return technologies;
        }

        Elements dataRows = technologyTable.select("tbody tr");
        logger.info("Found {} technology data rows", dataRows.size());

        for (Element row : dataRows) {
            Elements cells = row.select("td");
            if (cells.size() < 4) continue;

            try {
                String technologyType = cleanText(cells.get(0).text());
                Integer volumeOffered = parseVolume(cells.get(1).text());
                Integer volumeAllocated = parseVolume(cells.get(2).text());
                BigDecimal avgPrice = parsePrice(cells.get(3).text());

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

    private Element findTableByHeaders(Element parent, String... headers) {
        Elements tables = parent.select("table");

        for (Element table : tables) {
            String headerText = table.select("thead, th").text().toLowerCase();
            boolean hasAllHeaders = true;

            for (String header : headers) {
                if (!headerText.contains(header.toLowerCase())) {
                    hasAllHeaders = false;
                    break;
                }
            }

            if (hasAllHeaders) {
                return table;
            }
        }

        return null;
    }

    private String cleanText(String text) {
        return text.trim().replaceAll("\\s+", " ");
    }

    // Parse volume numbers that might have commas, dots, or other formatting
    private Integer parseVolume(String volumeText) {
        try {
            String cleaned = volumeText.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
        } catch (Exception e) {
            logger.debug("Could not parse volume: {}", volumeText);
            return null;
        }
    }

    // Parse price values that start with € symbol and may have decimal points
    private BigDecimal parsePrice(String priceText) {
        try {
            // Remove € symbol and other non-numeric characters except decimal points
            String cleaned = priceText.replaceAll("[^0-9.,]", "");
            // Handle both comma and dot as decimal separator
            cleaned = cleaned.replace(",", ".");

            // Handle case where there might be multiple dots (thousands separator)
            int lastDotIndex = cleaned.lastIndexOf(".");
            if (lastDotIndex > 0 && cleaned.length() - lastDotIndex <= 3) {
                // Likely a decimal point
                String beforeDecimal = cleaned.substring(0, lastDotIndex).replace(".", "");
                String afterDecimal = cleaned.substring(lastDotIndex);
                cleaned = beforeDecimal + afterDecimal;
            }

            return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
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