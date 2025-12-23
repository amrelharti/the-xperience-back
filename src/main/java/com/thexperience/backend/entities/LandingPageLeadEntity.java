package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "landing_page_lead")
public class LandingPageLeadEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;
    
    @Column(nullable = false, length = 50)
    private String phone;
    
    @Column(name = "looking_for", nullable = false, length = 100)
    private String lookingFor;
    
    @Column(nullable = false, length = 255)
    private String offer;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getLookingFor() {
        return lookingFor;
    }
    
    public void setLookingFor(String lookingFor) {
        this.lookingFor = lookingFor;
    }
    
    public String getOffer() {
        return offer;
    }
    
    public void setOffer(String offer) {
        this.offer = offer;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

