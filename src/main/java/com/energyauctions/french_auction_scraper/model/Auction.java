package com.energyauctions.french_auction_scraper.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_date", nullable = false)
    private LocalDate auctionDate;

    @Column(name = "production_month", nullable = false)
    private String productionMonth;

    @Column(name = "reserve_price")
    private BigDecimal reservePrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // One auction has many regional results
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuctionRegion> regions;

    // One auction has many technology results
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuctionTechnology> technologies;

    // Constructors
    public Auction() {}

    public Auction(LocalDate auctionDate, String productionMonth, BigDecimal reservePrice) {
        this.auctionDate = auctionDate;
        this.productionMonth = productionMonth;
        this.reservePrice = reservePrice;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getAuctionDate() {
        return auctionDate;
    }

    public void setAuctionDate(LocalDate auctionDate) {
        this.auctionDate = auctionDate;
    }

    public String getProductionMonth() {
        return productionMonth;
    }

    public void setProductionMonth(String productionMonth) {
        this.productionMonth = productionMonth;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<AuctionRegion> getRegions() {
        return regions;
    }

    public void setRegions(List<AuctionRegion> regions) {
        this.regions = regions;
    }

    public List<AuctionTechnology> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<AuctionTechnology> technologies) {
        this.technologies = technologies;
    }
}
