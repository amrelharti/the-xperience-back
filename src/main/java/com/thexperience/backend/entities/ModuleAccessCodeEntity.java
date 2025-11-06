package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Multi-use access codes for training modules, specific to each class.
 * Each code is tied to a specific TrainingModule + Class combination.
 * All students in the same class use the same code for each training module.
 */
@Entity
@Table(name = "module_access_code")
public class ModuleAccessCodeEntity {
    @Id @GeneratedValue
    private UUID id;

    // Link to parent training (kept for backward compatibility)
    @ManyToOne
    @JoinColumn(name = "module_id")
    private ModuleEntity module;
    
    // Link to specific training module (REQUIRED)
    @ManyToOne(optional = false)
    @JoinColumn(name = "training_module_id")
    private TrainingModuleEntity trainingModule;
    
    // Link to specific class (REQUIRED) - this code is only valid for this class
    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "plain_code", length = 20)
    private String plainCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne
    @JoinColumn(name = "generated_by")
    private UserEntity generatedBy;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public ModuleEntity getModule() { return module; }
    public void setModule(ModuleEntity module) { this.module = module; }
    
    public TrainingModuleEntity getTrainingModule() { return trainingModule; }
    public void setTrainingModule(TrainingModuleEntity trainingModule) { this.trainingModule = trainingModule; }
    
    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }
    
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    
    public String getPlainCode() { return plainCode; }
    public void setPlainCode(String plainCode) { this.plainCode = plainCode; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public UserEntity getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(UserEntity generatedBy) { this.generatedBy = generatedBy; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
