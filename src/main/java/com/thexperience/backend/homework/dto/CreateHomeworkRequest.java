package com.thexperience.backend.homework.dto;

import java.util.UUID;

public class CreateHomeworkRequest {
    private UUID moduleId;
    private String title;
    private String body;
    private String attachments;

    // Constructors
    public CreateHomeworkRequest() {}

    public CreateHomeworkRequest(UUID moduleId, String title, String body, String attachments) {
        this.moduleId = moduleId;
        this.title = title;
        this.body = body;
        this.attachments = attachments;
    }

    // Getters and setters
    public UUID getModuleId() { return moduleId; }
    public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
}
