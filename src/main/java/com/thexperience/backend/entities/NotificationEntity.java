package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType = NotificationType.POST_CREATED;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Notification types enum
    public enum NotificationType {
        POST_CREATED,    // New post was created
        COMMENT,        // Someone commented on your post
        LIKE,           // Someone liked your post
        RATING          // Someone rated your assignment/weekly summary
    }

    // Constructors
    public NotificationEntity() {}

    public NotificationEntity(UserEntity user, PostEntity post) {
        this.user = user;
        this.post = post;
        this.notificationType = NotificationType.POST_CREATED;
        this.isRead = false;
    }

    public NotificationEntity(UserEntity user, PostEntity post, NotificationType type) {
        this.user = user;
        this.post = post;
        this.notificationType = type;
        this.isRead = false;
    }

    public NotificationEntity(UserEntity user, PostEntity post, NotificationType type, UserEntity actor) {
        this(user, post, type);
        this.actor = actor;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public PostEntity getPost() { return post; }
    public void setPost(PostEntity post) { this.post = post; }

    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UserEntity getActor() { return actor; }
    public void setActor(UserEntity actor) { this.actor = actor; }
}

