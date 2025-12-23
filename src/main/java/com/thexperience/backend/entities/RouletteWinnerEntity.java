package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "roulette_winners")
public class RouletteWinnerEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "prize", nullable = false)
    private String prize;
    
    @Column(name = "prize_index", nullable = false)
    private Integer prizeIndex;
    
    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier; // Can be IP, session ID, or user ID
    
    @Column(name = "win_date", nullable = false)
    private LocalDate winDate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "verification_code", nullable = false, unique = true, length = 8)
    private String verificationCode;
    
    @Column(name = "winner_name")
    private String winnerName;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (winDate == null) {
            winDate = LocalDate.now();
        }
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getPrize() {
        return prize;
    }
    
    public void setPrize(String prize) {
        this.prize = prize;
    }
    
    public Integer getPrizeIndex() {
        return prizeIndex;
    }
    
    public void setPrizeIndex(Integer prizeIndex) {
        this.prizeIndex = prizeIndex;
    }
    
    public String getUserIdentifier() {
        return userIdentifier;
    }
    
    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }
    
    public LocalDate getWinDate() {
        return winDate;
    }
    
    public void setWinDate(LocalDate winDate) {
        this.winDate = winDate;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getVerificationCode() {
        return verificationCode;
    }
    
    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
    
    public String getWinnerName() {
        return winnerName;
    }
    
    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }
}

