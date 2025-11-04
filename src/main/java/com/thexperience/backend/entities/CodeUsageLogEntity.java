package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_usage_log")
public class CodeUsageLogEntity {
    @Id @GeneratedValue
    private UUID id;

    @Column(name = "code_id", nullable = false)
    private UUID codeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt = Instant.now();

    @Column(name = "code_type", nullable = false, length = 50)
    private String codeType; // "CLASS_ENROLLMENT" or "TRAINING_MODULE"

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId; // Class ID or Training Module ID

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getCodeId() { return codeId; }
    public void setCodeId(UUID codeId) { this.codeId = codeId; }
    
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    
    public String getCodeType() { return codeType; }
    public void setCodeType(String codeType) { this.codeType = codeType; }
    
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
}

