package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "live_session_attendance", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"}))
public class LiveSessionAttendanceEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private LiveSessionEntity session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @CreationTimestamp
    @Column(name = "attended_at")
    private Instant attendedAt;

    // Constructors
    public LiveSessionAttendanceEntity() {}

    public LiveSessionAttendanceEntity(LiveSessionEntity session, UserEntity user) {
        this.session = session;
        this.user = user;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LiveSessionEntity getSession() { return session; }
    public void setSession(LiveSessionEntity session) { this.session = session; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public Instant getAttendedAt() { return attendedAt; }
    public void setAttendedAt(Instant attendedAt) { this.attendedAt = attendedAt; }
}
