package com.energyauctions.french_auction_scraper.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_regions")
public class AuctionRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each region belongs to one auction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @JsonIgnore
    private Auction auction;

    @Column(name = "region_name", nullable = false)
    private String regionName;

    @Column(name = "volume_offered", nullable = false)
    private Integer volumeOffered;

    @Column(name = "volume_allocated", nullable = false)
    private Integer volumeAllocated;

    @Column(name = "weighted_avg_price", nullable = false)
    private BigDecimal weightedAvgPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuctionRegion() {}

    public AuctionRegion(Auction auction, String regionName, Integer volumeOffered,
                         Integer volumeAllocated, BigDecimal weightedAvgPrice) {
        this.auction = auction;
        this.regionName = regionName;
        this.volumeOffered = volumeOffered;
        this.volumeAllocated = volumeAllocated;
        this.weightedAvgPrice = weightedAvgPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public Integer getVolumeOffered() {
        return volumeOffered;
    }

    public void setVolumeOffered(Integer volumeOffered) {
        this.volumeOffered = volumeOffered;
    }

    public Integer getVolumeAllocated() {
        return volumeAllocated;
    }

    public void setVolumeAllocated(Integer volumeAllocated) {
        this.volumeAllocated = volumeAllocated;
    }

    public BigDecimal getWeightedAvgPrice() {
        return weightedAvgPrice;
    }

    public void setWeightedAvgPrice(BigDecimal weightedAvgPrice) {
        this.weightedAvgPrice = weightedAvgPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
