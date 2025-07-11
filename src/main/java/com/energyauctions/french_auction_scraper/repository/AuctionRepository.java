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

    Optional<Auction> findByAuctionDateAndProductionMonth(LocalDate auctionDate, String productionMonth);

    // Get the most recent auction - uses Spring Data JPA method naming convention
    // Regions and technologies will be loaded automatically when the frontend accesses them
    Optional<Auction> findTopByOrderByAuctionDateDesc();

    List<Auction> findByAuctionDateBetweenOrderByAuctionDateDesc(LocalDate startDate, LocalDate endDate);

    // This avoids the MultipleBagFetchException that occurs when trying to fetch multiple @OneToMany collections in one query
    @Query("SELECT a FROM Auction a ORDER BY a.auctionDate DESC")
    List<Auction> findAllAuctions();
}