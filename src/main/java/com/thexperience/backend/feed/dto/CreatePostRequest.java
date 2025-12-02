package com.thexperience.backend.feed.dto;

import com.thexperience.backend.entities.PostEntity;
import java.util.UUID;

public class CreatePostRequest {
    private UUID moduleId;
    private UUID trainingModuleId;
    private UUID classId;
    private String postType;
    private String title;
    private String content;
    private String attachments;
    private Boolean isAssignment;
    private Boolean shareToGlobal; // true = share to global feed, false = class-only

    // Constructors
    public CreatePostRequest() {}

    public CreatePostRequest(String postType, String content) {
        this.postType = postType;
        this.content = content;
    }

    // Getters and setters
    public UUID getModuleId() { return moduleId; }
    public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }

    public UUID getTrainingModuleId() { return trainingModuleId; }
    public void setTrainingModuleId(UUID trainingModuleId) { this.trainingModuleId = trainingModuleId; }

    public UUID getClassId() { return classId; }
    public void setClassId(UUID classId) { this.classId = classId; }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public Boolean getIsAssignment() { return isAssignment; }
    public void setIsAssignment(Boolean isAssignment) { this.isAssignment = isAssignment; }

    public Boolean getShareToGlobal() { return shareToGlobal; }
    public void setShareToGlobal(Boolean shareToGlobal) { this.shareToGlobal = shareToGlobal; }

    @Override
    public String toString() {
        return "CreatePostRequest{" +
                "moduleId=" + moduleId +
                ", trainingModuleId=" + trainingModuleId +
                ", classId=" + classId +
                ", postType=" + postType +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", attachments='" + attachments + '\'' +
                ", isAssignment=" + isAssignment +
                ", shareToGlobal=" + shareToGlobal +
                '}';
    }
}
