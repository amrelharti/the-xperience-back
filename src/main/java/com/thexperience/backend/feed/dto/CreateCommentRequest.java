package com.thexperience.backend.feed.dto;

import java.util.UUID;

public class CreateCommentRequest {
    private String content;
    private UUID parentCommentId;

    // Constructors
    public CreateCommentRequest() {}

    public CreateCommentRequest(String content) {
        this.content = content;
    }

    // Getters and setters
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public UUID getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(UUID parentCommentId) { this.parentCommentId = parentCommentId; }
}
