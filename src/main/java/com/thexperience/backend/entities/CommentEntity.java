package com.thexperience.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class CommentEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id")
    private PostEntity post;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private CommentEntity parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentEntity> replies = new ArrayList<>();

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentLikeEntity> likes = new ArrayList<>();

    // Constructors
    public CommentEntity() {}

    public CommentEntity(PostEntity post, UserEntity author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
    }

    public CommentEntity(PostEntity post, UserEntity author, String content, CommentEntity parentComment) {
        this.post = post;
        this.author = author;
        this.content = content;
        this.parentComment = parentComment;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PostEntity getPost() { return post; }
    public void setPost(PostEntity post) { this.post = post; }

    public UserEntity getAuthor() { return author; }
    public void setAuthor(UserEntity author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public CommentEntity getParentComment() { return parentComment; }
    public void setParentComment(CommentEntity parentComment) { this.parentComment = parentComment; }

    public List<CommentEntity> getReplies() { return replies; }
    public void setReplies(List<CommentEntity> replies) { this.replies = replies; }

    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<CommentLikeEntity> getLikes() { return likes; }
    public void setLikes(List<CommentLikeEntity> likes) { this.likes = likes; }
}
