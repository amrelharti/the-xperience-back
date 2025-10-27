package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * TrainingModuleEntity - Represents a MODULE/WEEK within a TRAINING
 * Hierarchy: Training (ModuleEntity) → TrainingModule → Course
 * All classes in a training share the same modules (curriculum)
 * Example: "Module 1", "Module 2", etc. in "Full Stack Development" training
 */
@Entity
@Table(name = "training_modules")
public class TrainingModuleEntity {
    
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_id", nullable = false)
    private ModuleEntity training;

    @Column(nullable = false)
    private String title; // e.g., "Module 1", "Module 2"

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0; // Sequential order: 1, 2, 3...

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "trainingModule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CourseEntity> courses; // Video lessons in this module

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Constructors
    public TrainingModuleEntity() {}

    public TrainingModuleEntity(ModuleEntity training, String title, String description, Integer orderIndex) {
        this.training = training;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ModuleEntity getTraining() { return training; }
    public void setTraining(ModuleEntity training) { this.training = training; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public List<CourseEntity> getCourses() { return courses; }
    public void setCourses(List<CourseEntity> courses) { this.courses = courses; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

