package com.thexperience.backend.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classes")
public class ClassEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g., "October 2024 Class"

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private UserEntity createdBy; // Admin who created the class

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_archived", nullable = false)
    private Boolean isArchived = false;

    @Column(name = "max_students")
    private Integer maxStudents; // null = no limit

    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CourseProgressEntity> courseProgress;
    
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PostEntity> posts;
    
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClassEnrollmentEntity> enrollments;
    
    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LiveSessionAttendanceEntity> liveSessionAttendance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Constructors
    public ClassEntity() {}

    public ClassEntity(String name, String description, ModuleEntity module, UserEntity createdBy, 
                      LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.module = module;
        this.createdBy = createdBy;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ModuleEntity getModule() { return module; }
    public void setModule(ModuleEntity module) { this.module = module; }

    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<CourseProgressEntity> getCourseProgress() { return courseProgress; }
    public void setCourseProgress(List<CourseProgressEntity> courseProgress) { this.courseProgress = courseProgress; }
    public List<PostEntity> getPosts() { return posts; }
    public void setPosts(List<PostEntity> posts) { this.posts = posts; }
    public List<ClassEnrollmentEntity> getEnrollments() { return enrollments; }
    public void setEnrollments(List<ClassEnrollmentEntity> enrollments) { this.enrollments = enrollments; }
    public List<LiveSessionAttendanceEntity> getLiveSessionAttendance() { return liveSessionAttendance; }
    public void setLiveSessionAttendance(List<LiveSessionAttendanceEntity> liveSessionAttendance) { this.liveSessionAttendance = liveSessionAttendance; }
}
