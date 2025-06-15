package com.energyauctions.french_auction_scraper.controller;



import com.energyauctions.french_auction_scraper.model.Auction;
import com.energyauctions.french_auction_scraper.repository.AuctionRepository;
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
@CrossOrigin(origins = "http://localhost:3000") // Allow React app to connect
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    // Get all auction data with regions and technologies
    @GetMapping
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAllAuctions();
    }

    // Get the latest auction results
    @GetMapping("/latest")
    public ResponseEntity<Auction> getLatestAuction() {
        return auctionRepository.findLatestWithDetails()
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

                // Load the latest with details to get counts
                Auction latestWithDetails = auctionRepository.findLatestWithDetails().orElse(null);
                if (latestWithDetails != null) {
                    if (latestWithDetails.getRegions() != null) {
                        stats.put("regionsCount", latestWithDetails.getRegions().size());
                    }
                    if (latestWithDetails.getTechnologies() != null) {
                        stats.put("technologiesCount", latestWithDetails.getTechnologies().size());
                    }
                }
            }
        }

        return ResponseEntity.ok(stats);
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
