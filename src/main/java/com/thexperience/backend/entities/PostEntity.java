package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts")
public class PostEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private ModuleEntity module;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false)
    private PostType postType = PostType.POST;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "is_assignment")
    private Boolean isAssignment = false;

    @ManyToOne
    @JoinColumn(name = "assignment_module_id")
    private ModuleEntity assignmentModule;

    @Column(name = "avg_score", precision = 3, scale = 2)
    private BigDecimal avgScore = BigDecimal.ZERO;

    @Column(name = "score_count")
    private Integer scoreCount = 0;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentEntity> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostLikeEntity> likes = new ArrayList<>();

    // Constructors
    public PostEntity() {}

    public PostEntity(UserEntity author, String content) {
        this.author = author;
        this.content = content;
    }

    public PostEntity(UserEntity author, ModuleEntity module, PostType postType, String title, String content) {
        this.author = author;
        this.module = module;
        this.postType = postType;
        this.title = title;
        this.content = content;
    }

    // Enums
    public enum PostType {
        POST, ASSIGNMENT, QUESTION, ANNOUNCEMENT
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getAuthor() { return author; }
    public void setAuthor(UserEntity author) { this.author = author; }

    public ModuleEntity getModule() { return module; }
    public void setModule(ModuleEntity module) { this.module = module; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public PostType getPostType() { return postType; }
    public void setPostType(PostType postType) { this.postType = postType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public Boolean getIsAssignment() { return isAssignment; }
    public void setIsAssignment(Boolean isAssignment) { this.isAssignment = isAssignment; }

    public ModuleEntity getAssignmentModule() { return assignmentModule; }
    public void setAssignmentModule(ModuleEntity assignmentModule) { this.assignmentModule = assignmentModule; }

    public BigDecimal getAvgScore() { return avgScore; }
    public void setAvgScore(BigDecimal avgScore) { this.avgScore = avgScore; }

    public Integer getScoreCount() { return scoreCount; }
    public void setScoreCount(Integer scoreCount) { this.scoreCount = scoreCount; }

    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }

    public Boolean getIsPinned() { return isPinned; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<CommentEntity> getComments() { return comments; }
    public void setComments(List<CommentEntity> comments) { this.comments = comments; }

    public List<PostLikeEntity> getLikes() { return likes; }
    public void setLikes(List<PostLikeEntity> likes) { this.likes = likes; }
}
