package com.energyauctions.french_auction_scraper.model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_technologies")
public class AuctionTechnology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Each technology record belongs to one auction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @JsonIgnore
    private Auction auction;

    @Column(name = "technology_type", nullable = false)
    private String technologyType;

    @Column(name = "volume_offered", nullable = false)
    private Integer volumeOffered;

    @Column(name = "volume_allocated", nullable = false)
    private Integer volumeAllocated;

    @Column(name = "weighted_avg_price", nullable = false)
    private BigDecimal weightedAvgPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


    public AuctionTechnology() {}

    public AuctionTechnology(Auction auction, String technologyType, Integer volumeOffered,
                             Integer volumeAllocated, BigDecimal weightedAvgPrice) {
        this.auction = auction;
        this.technologyType = technologyType;
        this.volumeOffered = volumeOffered;
        this.volumeAllocated = volumeAllocated;
        this.weightedAvgPrice = weightedAvgPrice;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public String getTechnologyType() {
        return technologyType;
    }

    public void setTechnologyType(String technologyType) {
        this.technologyType = technologyType;
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
