package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_access_codes")
public class CourseAccessCodeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private UserEntity generatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumed_by")
    private UserEntity consumedBy;

    @Column(name = "is_single_use", nullable = false)
    private boolean isSingleUse = true;

    // Constructors
    public CourseAccessCodeEntity() {}

    public CourseAccessCodeEntity(CourseEntity course, String codeHash, UserEntity generatedBy) {
        this.course = course;
        this.codeHash = codeHash;
        this.generatedBy = generatedBy;
        this.createdAt = Instant.now();
        this.isSingleUse = true;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public CourseEntity getCourse() { return course; }
    public void setCourse(CourseEntity course) { this.course = course; }

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

    public boolean isSingleUse() { return isSingleUse; }
    public void setSingleUse(boolean isSingleUse) { this.isSingleUse = isSingleUse; }
}

