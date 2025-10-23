package com.thexperience.backend.classes.dto;

import java.time.Instant;
import java.util.UUID;

public class ClassStudentResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private Instant enrolledAt;

    // Constructors
    public ClassStudentResponse() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}


