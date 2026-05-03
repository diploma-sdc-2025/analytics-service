package org.java.diploma.service.analyticsservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "player_statistics")
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_matches_played", nullable = false)
    private int totalMatchesPlayed;

    @Column(name = "total_matches_won", nullable = false)
    private int totalMatchesWon;

    @Column(name = "total_battles_fought", nullable = false)
    private int totalBattlesFought;

    @Column(name = "total_pieces_purchased", nullable = false)
    private int totalPiecesPurchased;

    @Column(name = "total_gold_spent", nullable = false)
    private int totalGoldSpent;

    @Column(name = "avg_match_duration_seconds", nullable = false)
    private BigDecimal avgMatchDurationSeconds = BigDecimal.ZERO;

    @Column(name = "highest_round_reached", nullable = false)
    private int highestRoundReached;

    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal winRate = BigDecimal.ZERO;

    @Column(name = "current_rating", nullable = false)
    private int currentRating = 1000;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @PrePersist
    @PreUpdate
    void touch() {
        lastUpdated = Instant.now();
    }

    public void recomputeWinRate() {
        if (totalMatchesPlayed <= 0) {
            this.winRate = BigDecimal.ZERO;
            return;
        }
        this.winRate = BigDecimal.valueOf(totalMatchesWon)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalMatchesPlayed), 2, RoundingMode.HALF_UP);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getTotalMatchesPlayed() {
        return totalMatchesPlayed;
    }

    public void setTotalMatchesPlayed(int totalMatchesPlayed) {
        this.totalMatchesPlayed = totalMatchesPlayed;
    }

    public int getTotalMatchesWon() {
        return totalMatchesWon;
    }

    public void setTotalMatchesWon(int totalMatchesWon) {
        this.totalMatchesWon = totalMatchesWon;
    }

    public int getTotalBattlesFought() {
        return totalBattlesFought;
    }

    public void setTotalBattlesFought(int totalBattlesFought) {
        this.totalBattlesFought = totalBattlesFought;
    }

    public int getTotalPiecesPurchased() {
        return totalPiecesPurchased;
    }

    public void setTotalPiecesPurchased(int totalPiecesPurchased) {
        this.totalPiecesPurchased = totalPiecesPurchased;
    }

    public int getTotalGoldSpent() {
        return totalGoldSpent;
    }

    public void setTotalGoldSpent(int totalGoldSpent) {
        this.totalGoldSpent = totalGoldSpent;
    }

    public BigDecimal getAvgMatchDurationSeconds() {
        return avgMatchDurationSeconds;
    }

    public void setAvgMatchDurationSeconds(BigDecimal avgMatchDurationSeconds) {
        this.avgMatchDurationSeconds = avgMatchDurationSeconds;
    }

    public int getHighestRoundReached() {
        return highestRoundReached;
    }

    public void setHighestRoundReached(int highestRoundReached) {
        this.highestRoundReached = highestRoundReached;
    }

    public BigDecimal getWinRate() {
        return winRate;
    }

    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }

    public int getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(int currentRating) {
        this.currentRating = currentRating;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
