package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "module_access_code")
public class ModuleAccessCodeEntity {
    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "module_id")
    private ModuleEntity module;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ModuleEntity getModule() { return module; }
    public void setModule(ModuleEntity module) { this.module = module; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
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
}
