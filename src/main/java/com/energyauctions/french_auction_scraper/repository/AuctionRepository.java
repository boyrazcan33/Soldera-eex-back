package com.energyauctions.french_auction_scraper.repository;


import com.energyauctions.french_auction_scraper.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    // Find auction by date and production month
    Optional<Auction> findByAuctionDateAndProductionMonth(LocalDate auctionDate, String productionMonth);

    // Get the most recent auction
    Optional<Auction> findTopByOrderByAuctionDateDesc();

    // Get auctions in date range
    List<Auction> findByAuctionDateBetweenOrderByAuctionDateDesc(LocalDate startDate, LocalDate endDate);

    // Get all auctions with their regions and technologies loaded (avoids N+1 query problem)
    @Query("SELECT DISTINCT a FROM Auction a " +
            "LEFT JOIN FETCH a.regions " +
            "LEFT JOIN FETCH a.technologies " +
            "ORDER BY a.auctionDate DESC")
    List<Auction> findAllWithDetails();

    // Get latest auction with all details
    @Query("SELECT a FROM Auction a " +
            "LEFT JOIN FETCH a.regions " +
            "LEFT JOIN FETCH a.technologies " +
            "WHERE a.auctionDate = (SELECT MAX(a2.auctionDate) FROM Auction a2)")
    Optional<Auction> findLatestWithDetails();
}
