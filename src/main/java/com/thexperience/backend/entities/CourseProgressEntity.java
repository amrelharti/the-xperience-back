package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id", "class_id"}))
public class CourseProgressEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Column(nullable = false)
    private String status = "NOT_STARTED"; // NOT_STARTED | IN_PROGRESS | COMPLETED

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "watch_time_seconds")
    private Integer watchTimeSeconds = 0; // Track how much of the video was watched

    @Column(name = "completion_percentage")
    private Double completionPercentage = 0.0; // Percentage of course completed

    // Constructors
    public CourseProgressEntity() {}

    public CourseProgressEntity(UserEntity user, CourseEntity course, ClassEntity classEntity) {
        this.user = user;
        this.course = course;
        this.classEntity = classEntity;
        this.status = "NOT_STARTED";
        this.startedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public CourseEntity getCourse() { return course; }
    public void setCourse(CourseEntity course) { this.course = course; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Integer getWatchTimeSeconds() { return watchTimeSeconds; }
    public void setWatchTimeSeconds(Integer watchTimeSeconds) { this.watchTimeSeconds = watchTimeSeconds; }

    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    // Helper methods
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isStarted() {
        return !"NOT_STARTED".equals(status);
    }

    public void markAsCompleted() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
        this.completionPercentage = 100.0;
    }

    public void markAsStarted() {
        if ("NOT_STARTED".equals(status)) {
            this.status = "IN_PROGRESS";
            this.startedAt = Instant.now();
        }
    }
}


