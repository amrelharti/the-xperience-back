package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_enrollment_code")
public class ClassEnrollmentCodeEntity {
    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "plain_code", length = 20)
    private String plainCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private UserEntity generatedBy;

    @ManyToOne
    @JoinColumn(name = "consumed_by")
    private UserEntity consumedBy;

    @Column(name = "is_single_use", nullable = false)
    private boolean singleUse = true;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public String getPlainCode() { return plainCode; }
    public void setPlainCode(String plainCode) { this.plainCode = plainCode; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }
    public UserEntity getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(UserEntity generatedBy) { this.generatedBy = generatedBy; }
    public UserEntity getConsumedBy() { return consumedBy; }
    public void setConsumedBy(UserEntity consumedBy) { this.consumedBy = consumedBy; }
    public boolean isSingleUse() { return singleUse; }
    public void setSingleUse(boolean singleUse) { this.singleUse = singleUse; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}


