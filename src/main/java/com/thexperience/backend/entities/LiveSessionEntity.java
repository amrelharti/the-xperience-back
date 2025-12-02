package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "live_sessions")
public class LiveSessionEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "training_module_id")
    private TrainingModuleEntity trainingModule;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    private String location;

    @Column(name = "meeting_url", columnDefinition = "TEXT")
    private String meetingUrl; // Zoom meeting link

    @Column(name = "meeting_password")
    private String meetingPassword; // Optional Zoom password

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Session duration in minutes

    @Column(name = "instructor_name")
    private String instructorName; // Instructor name

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LiveSessionAttendanceEntity> attendance = new ArrayList<>();

    // Constructors
    public LiveSessionEntity() {}

    public LiveSessionEntity(TrainingModuleEntity trainingModule, ClassEntity classEntity, String title, String description, 
                           Instant startsAt, Instant endsAt, String location) {
        this.trainingModule = trainingModule;
        this.classEntity = classEntity;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.isArchived = false;
    }

    public LiveSessionEntity(TrainingModuleEntity trainingModule, ClassEntity classEntity, String title, String description, 
                           Instant startsAt, Instant endsAt, String location,
                           String meetingUrl, String meetingPassword, Integer durationMinutes, String instructorName) {
        this.trainingModule = trainingModule;
        this.classEntity = classEntity;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
        this.meetingUrl = meetingUrl;
        this.meetingPassword = meetingPassword;
        this.durationMinutes = durationMinutes;
        this.instructorName = instructorName;
        this.isArchived = false;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TrainingModuleEntity getTrainingModule() { return trainingModule; }
    public void setTrainingModule(TrainingModuleEntity trainingModule) { this.trainingModule = trainingModule; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }

    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<LiveSessionAttendanceEntity> getAttendance() { return attendance; }
    public void setAttendance(List<LiveSessionAttendanceEntity> attendance) { this.attendance = attendance; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getMeetingPassword() { return meetingPassword; }
    public void setMeetingPassword(String meetingPassword) { this.meetingPassword = meetingPassword; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
}
