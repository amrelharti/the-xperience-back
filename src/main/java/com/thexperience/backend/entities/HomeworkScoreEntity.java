package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "homework_scores", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"homework_id", "user_id"}))
public class HomeworkScoreEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "homework_id")
    private HomeworkEntity homework;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(nullable = false)
    private Integer value; // 0-10

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors
    public HomeworkScoreEntity() {}

    public HomeworkScoreEntity(HomeworkEntity homework, UserEntity user, Integer value) {
        this.homework = homework;
        this.user = user;
        this.value = value;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public HomeworkEntity getHomework() { return homework; }
    public void setHomework(HomeworkEntity homework) { this.homework = homework; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
