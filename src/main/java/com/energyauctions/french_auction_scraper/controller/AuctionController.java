package com.energyauctions.french_auction_scraper.controller;

import com.energyauctions.french_auction_scraper.model.Auction;
import com.energyauctions.french_auction_scraper.repository.AuctionRepository;
import com.energyauctions.french_auction_scraper.service.EEXAuctionScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = {"https://energy-auction-frontend-640616451900.europe-north1.run.app", "http://localhost:3000"})
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private EEXAuctionScraperService scraperService;

    // Get all auction data with regions and technologies
    @GetMapping
    public List<Auction> getAllAuctions() {
        // Changed from findAllWithDetails() to avoid Hibernate's MultipleBagFetchException
        // The regions and technologies will still be available in the JSON response through lazy loading
        return auctionRepository.findAllAuctions();
    }

    // Get the latest auction results
    @GetMapping("/latest")
    public ResponseEntity<Auction> getLatestAuction() {
        return auctionRepository.findTopByOrderByAuctionDateDesc()
                .map(auction -> ResponseEntity.ok(auction))
                .orElse(ResponseEntity.notFound().build());
    }

    // Get auctions in a date range
    @GetMapping("/range")
    public List<Auction> getAuctionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return auctionRepository.findByAuctionDateBetweenOrderByAuctionDateDesc(startDate, endDate);
    }

    // Get regional data for charts
    @GetMapping("/regions")
    public ResponseEntity<Map<String, Object>> getRegionalData() {
        // Changed from findAllWithDetails() to avoid Hibernate's MultipleBagFetchException
        // The regions and technologies will still be available in the JSON response through lazy loading
        List<Auction> auctions = auctionRepository.findAllAuctions();

        if (auctions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("auctions", auctions);
        response.put("totalAuctions", auctions.size());

        return ResponseEntity.ok(response);
    }

    // Get technology breakdown data
    @GetMapping("/technologies")
    public ResponseEntity<Map<String, Object>> getTechnologyData() {
        // Changed from findAllWithDetails() to avoid Hibernate's MultipleBagFetchException
        // The regions and technologies will still be available in the JSON response through lazy loading
        List<Auction> auctions = auctionRepository.findAllAuctions();

        if (auctions.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("auctions", auctions);
        response.put("totalAuctions", auctions.size());

        return ResponseEntity.ok(response);
    }

    // Get basic stats for dashboard
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<Auction> auctions = auctionRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAuctions", auctions.size());

        if (!auctions.isEmpty()) {
            // Get latest auction for additional stats
            Auction latest = auctionRepository.findTopByOrderByAuctionDateDesc().orElse(null);
            if (latest != null) {
                stats.put("latestAuctionDate", latest.getAuctionDate());
                stats.put("latestProductionMonth", latest.getProductionMonth());

                // Changed from findLatestWithDetails() to simple findTop method
                // This prevents the MultipleBagFetchException when loading related collections
                // The regions and technologies will be loaded when accessed due to lazy loading
                if (latest.getRegions() != null) {
                    stats.put("regionsCount", latest.getRegions().size());
                }
                if (latest.getTechnologies() != null) {
                    stats.put("technologiesCount", latest.getTechnologies().size());
                }
            }
        }

        return ResponseEntity.ok(stats);
    }

    // Manual trigger for scraping - for testing and immediate updates
    @PostMapping("/scrape")
    public ResponseEntity<Map<String, String>> triggerScraping() {
        try {
            scraperService.scrapeNow();

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Scraping completed successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Scraping failed: " + e.getMessage());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Simple health check
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Auction API is running");
        return ResponseEntity.ok(response);
    }
}