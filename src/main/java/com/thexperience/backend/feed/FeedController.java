package com.thexperience.backend.feed;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.PostRepository;
import com.thexperience.backend.repos.CommentRepository;
import com.thexperience.backend.repos.PostLikeRepository;
import com.thexperience.backend.repos.CommentLikeRepository;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.feed.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;
    
    @Autowired
    private com.thexperience.backend.repos.ClassEnrollmentRepository classEnrollmentRepository;
    
    @Autowired
    private com.thexperience.backend.repos.ClassRepository classRepository;

    // Debug endpoint to check post module associations
    @GetMapping("/debug/posts")
    public ResponseEntity<?> debugPosts(Authentication auth) {
        try {
            List<PostEntity> allPosts = postRepository.findAll();
            List<Map<String, Object>> debugInfo = allPosts.stream()
                .map(post -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", post.getId());
                    info.put("content", post.getContent().substring(0, Math.min(50, post.getContent().length())));
                    info.put("moduleId", post.getModule() != null ? post.getModule().getId() : "null");
                    info.put("classId", post.getClassEntity() != null ? post.getClassEntity().getId() : "null");
                    info.put("className", post.getClassEntity() != null ? post.getClassEntity().getName() : "null");
                    info.put("isAssignment", post.getIsAssignment());
                    return info;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get feed posts - AUTOMATICALLY FILTERED BY USER'S CLASS
    @GetMapping
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String moduleId,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String search,
            Authentication auth) {
        try {
            System.out.println("=== FEED REQUEST (CLASS-FILTERED) ===");
            System.out.println("Request timestamp: " + java.time.Instant.now());
            System.out.println("Raw request parameters - moduleId: " + moduleId + ", classId: " + classId + ", type: " + type + ", search: " + search);
            System.out.println("Parameter types - moduleId: " + (moduleId != null ? moduleId.getClass().getSimpleName() : "null") + 
                             ", classId: " + (classId != null ? classId.getClass().getSimpleName() : "null"));
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            // Convert string parameters to UUIDs
            UUID moduleIdUuid = null;
            UUID classIdUuid = null;
            
            if (moduleId != null && !moduleId.trim().isEmpty()) {
                try {
                    moduleIdUuid = UUID.fromString(moduleId);
                    System.out.println("Converted moduleId to UUID: " + moduleIdUuid);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid moduleId format: " + moduleId);
                }
            }
            
            if (classId != null && !classId.trim().isEmpty()) {
                try {
                    classIdUuid = UUID.fromString(classId);
                    System.out.println("Converted classId to UUID: " + classIdUuid);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid classId format: " + classId);
                }
            }
            
            // Get user's enrolled classes automatically
            List<com.thexperience.backend.entities.ClassEnrollmentEntity> enrollments = 
                classEnrollmentRepository.findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(user);
            
            List<UUID> userClassIds = enrollments.stream()
                .map(e -> e.getClassEntity().getId())
                .collect(Collectors.toList());
            
            System.out.println("User: " + user.getName() + ", Enrolled in " + userClassIds.size() + " classes");
            System.out.println("User's enrolled class IDs: " + userClassIds);
            System.out.println("Requested classId: " + classIdUuid);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<PostEntity> posts = Page.empty(pageable); // Initialize with empty page

            // AUTOMATIC CLASS FILTERING - Show only posts from user's enrolled classes
            if (userClassIds.isEmpty()) {
                // No enrollments = no posts
                System.out.println("User has no class enrollments, returning empty feed");
                posts = Page.empty(pageable);
            } else {
                // Determine which classes to filter by
                List<UUID> classesToFilter;
                if (classIdUuid != null) {
                    // User selected a specific class - check if they're enrolled in it
                    System.out.println("Checking if user is enrolled in class: " + classIdUuid);
                    System.out.println("User's enrolled classes: " + userClassIds);
                    boolean isEnrolled = userClassIds.contains(classIdUuid);
                    System.out.println("Is user enrolled in requested class? " + isEnrolled);
                    
                    if (isEnrolled) {
                        classesToFilter = List.of(classIdUuid);
                        System.out.println("✅ Filtering by specific class: " + classIdUuid);
                    } else {
                        // User not enrolled in selected class - return empty
                        System.out.println("❌ User not enrolled in selected class, returning empty feed");
                        posts = Page.empty(pageable);
                        classesToFilter = List.of();
                    }
                } else {
                    // Show all user's classes
                    classesToFilter = userClassIds;
                    System.out.println("Loading posts from user's classes: " + userClassIds);
                }
                
                if (!classesToFilter.isEmpty()) {
                    if (search != null && !search.trim().isEmpty()) {
                        // Search within selected classes
                        System.out.println("Searching posts in selected classes with query: " + search);
                        if (moduleIdUuid != null) {
                            try {
                                posts = postRepository.findByClassEntityIdInAndModuleIdAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, moduleIdUuid, pageable);
                            } catch (Exception e) {
                                System.out.println("Error filtering by module in search, falling back to class-only filter: " + e.getMessage());
                                posts = postRepository.findByClassEntityIdInAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, pageable);
                            }
                        } else {
                            posts = postRepository.findByClassEntityIdInAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, pageable);
                        }
                        // Additional content filtering for search
                        List<PostEntity> allPosts = posts.getContent();
                        List<PostEntity> filteredPosts = allPosts.stream()
                            .filter(p -> p.getContent().toLowerCase().contains(search.trim().toLowerCase()) ||
                                       (p.getTitle() != null && p.getTitle().toLowerCase().contains(search.trim().toLowerCase())))
                            .collect(Collectors.toList());
                        posts = new org.springframework.data.domain.PageImpl<>(filteredPosts, pageable, filteredPosts.size());
                    } else if (type != null && !type.isEmpty()) {
                        // Filter by type within selected classes
                        PostEntity.PostType postType = PostEntity.PostType.valueOf(type.toUpperCase());
                        System.out.println("Filtering by type: " + postType + " in selected classes");
                        if (moduleIdUuid != null) {
                            try {
                                posts = postRepository.findByClassEntityIdInAndModuleIdAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, moduleIdUuid, postType, pageable);
                            } catch (Exception e) {
                                System.out.println("Error filtering by module and type, falling back to class and type filter: " + e.getMessage());
                                posts = postRepository.findByClassEntityIdInAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, postType, pageable);
                            }
                        } else {
                            posts = postRepository.findByClassEntityIdInAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, postType, pageable);
                        }
                    } else {
                        // Show all posts from selected classes
                        System.out.println("Loading all posts from selected classes");
                        // For now, let's use class-only filtering since posts might not have proper module association
                        System.out.println("Loading all posts from classes: " + classesToFilter);
                        posts = postRepository.findByClassEntityIdInAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(classesToFilter, pageable);
                        System.out.println("Found " + posts.getTotalElements() + " posts for classes");
                        
                        // TODO: Implement proper module filtering once posts have correct module association
                        if (moduleIdUuid != null) {
                            System.out.println("Note: Module filtering (" + moduleIdUuid + ") not yet implemented due to data structure issues");
                        }
                    }
                }
            }

            List<Map<String, Object>> postList = posts.getContent().stream()
                .map(this::convertPostToResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postList);
            response.put("totalPages", posts.getTotalPages());
            response.put("totalElements", posts.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch feed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Admin feed - shows all posts from all classes
    @GetMapping("/admin")
    public ResponseEntity<?> getAdminFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String classId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            Authentication auth) {
        try {
            System.out.println("=== ADMIN FEED REQUEST ===");
            
            // Verify admin role
            UserEntity user = userRepository.findById(UUID.fromString(auth.getName())).orElse(null);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<PostEntity> posts = Page.empty(pageable);
            
            if (classId != null && !classId.trim().isEmpty()) {
                // Filter by specific class
                System.out.println("Admin filtering by class: " + classId);
                if (search != null && !search.trim().isEmpty()) {
                    posts = postRepository.findByClassEntityIdAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(UUID.fromString(classId), pageable);
                    // Additional content filtering for search
                    List<PostEntity> allPosts = posts.getContent();
                    List<PostEntity> filteredPosts = allPosts.stream()
                        .filter(p -> p.getContent().toLowerCase().contains(search.trim().toLowerCase()) ||
                                   (p.getTitle() != null && p.getTitle().toLowerCase().contains(search.trim().toLowerCase())))
                        .collect(Collectors.toList());
                    posts = new org.springframework.data.domain.PageImpl<>(filteredPosts, pageable, filteredPosts.size());
                } else if (type != null && !type.isEmpty()) {
                    PostEntity.PostType postType = PostEntity.PostType.valueOf(type.toUpperCase());
                    posts = postRepository.findByClassEntityIdAndPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(UUID.fromString(classId), postType, pageable);
                } else {
                    posts = postRepository.findByClassEntityIdAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(UUID.fromString(classId), pageable);
                }
            } else {
                // Show all posts from all classes
                System.out.println("Admin loading all posts from all classes");
                if (search != null && !search.trim().isEmpty()) {
                    posts = postRepository.findByIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(pageable);
                    // Additional content filtering for search
                    List<PostEntity> allPosts = posts.getContent();
                    List<PostEntity> filteredPosts = allPosts.stream()
                        .filter(p -> p.getContent().toLowerCase().contains(search.trim().toLowerCase()) ||
                                   (p.getTitle() != null && p.getTitle().toLowerCase().contains(search.trim().toLowerCase())))
                        .collect(Collectors.toList());
                    posts = new org.springframework.data.domain.PageImpl<>(filteredPosts, pageable, filteredPosts.size());
                } else if (type != null && !type.isEmpty()) {
                    PostEntity.PostType postType = PostEntity.PostType.valueOf(type.toUpperCase());
                    posts = postRepository.findByPostTypeAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(postType, pageable);
                } else {
                    posts = postRepository.findByIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(pageable);
                }
            }

            List<Map<String, Object>> postList = posts.getContent().stream()
                .map(this::convertPostToResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postList);
            response.put("totalPages", posts.getTotalPages());
            response.put("totalElements", posts.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch admin feed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Create post
    @PostMapping("/posts")
    @Transactional
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request, Authentication auth) {
        try {
            System.out.println("Received create post request: " + request);
            System.out.println("PostType: " + request.getPostType());
            System.out.println("Content: " + request.getContent());
            System.out.println("ModuleId: " + request.getModuleId());
            
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();

            PostEntity post = new PostEntity(user, request.getContent());
            PostEntity.PostType postType = PostEntity.PostType.POST;
            if (request.getPostType() != null && !request.getPostType().trim().isEmpty()) {
                try {
                    postType = PostEntity.PostType.valueOf(request.getPostType().toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid post type: " + request.getPostType() + ", using default POST");
                }
            }
            post.setPostType(postType);
            post.setTitle(request.getTitle());
            post.setAttachments(request.getAttachments());
            
            // Automatically set isAssignment to true for ASSIGNMENT posts
            // This ensures all assignment submissions count for the leaderboard
            if (postType == PostEntity.PostType.ASSIGNMENT) {
                post.setIsAssignment(true);
                System.out.println("Auto-setting isAssignment=true for ASSIGNMENT post");
            } else {
                post.setIsAssignment(request.getIsAssignment() != null ? request.getIsAssignment() : false);
            }
            
            // Ensure timestamps are set
            if (post.getCreatedAt() == null) {
                post.setCreatedAt(Instant.now());
            }
            if (post.getUpdatedAt() == null) {
                post.setUpdatedAt(Instant.now());
            }

            System.out.println("Post created with postType: " + post.getPostType());
            System.out.println("Post isAssignment: " + post.getIsAssignment());

            // Set module if provided
            if (request.getModuleId() != null) {
                System.out.println("Setting module for post");
                ModuleEntity module = moduleRepository.findById(request.getModuleId()).orElseThrow();
                post.setModule(module);
                if (post.getIsAssignment()) {
                    post.setAssignmentModule(module);
                }
            }
            
            // ALWAYS AUTOMATICALLY SET CLASS - Critical for class-based feed and leaderboard!
            System.out.println("=== AUTO-ASSIGNING CLASS ===");
            List<com.thexperience.backend.entities.ClassEnrollmentEntity> enrollments = 
                classEnrollmentRepository.findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(user);
            
            System.out.println("User has " + enrollments.size() + " class enrollments");
            
            if (!enrollments.isEmpty()) {
                // If moduleId is provided, try to match the class with that module
                if (request.getModuleId() != null) {
                    boolean classAssigned = false;
                    for (com.thexperience.backend.entities.ClassEnrollmentEntity enrollment : enrollments) {
                        if (enrollment.getClassEntity().getModule() != null && 
                            enrollment.getClassEntity().getModule().getId().equals(request.getModuleId())) {
                            post.setClassEntity(enrollment.getClassEntity());
                            System.out.println("✅ Auto-assigned post to class (via module): " + enrollment.getClassEntity().getName());
                            classAssigned = true;
                            break;
                        }
                    }
                    // If no matching class found for module, use first enrollment
                    if (!classAssigned) {
                        post.setClassEntity(enrollments.get(0).getClassEntity());
                        System.out.println("✅ Auto-assigned post to first class: " + enrollments.get(0).getClassEntity().getName());
                    }
                } else {
                    // No moduleId provided - use first enrollment (most common case)
                    post.setClassEntity(enrollments.get(0).getClassEntity());
                    System.out.println("✅ Auto-assigned post to first class: " + enrollments.get(0).getClassEntity().getName());
                }
            } else {
                System.out.println("⚠️ User has no class enrollments!");
            }

            System.out.println("Saving post to database...");
            post = postRepository.save(post);
            System.out.println("Post saved successfully with ID: " + post.getId());
            return ResponseEntity.ok(convertPostToResponse(post));
        } catch (Exception e) {
            System.out.println("Error creating post: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create post: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get post details
    @GetMapping("/posts/{postId}")
    public ResponseEntity<?> getPostDetails(@PathVariable UUID postId, Authentication auth) {
        try {
            PostEntity post = postRepository.findById(postId).orElseThrow();
            
            if (post.getIsHidden()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Post not found");
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(convertPostToResponse(post));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch post: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get post comments
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> getPostComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            PostEntity post = postRepository.findById(postId).orElseThrow();
            Pageable pageable = PageRequest.of(page, size);
            Page<CommentEntity> comments = commentRepository.findByPostOrderByCreatedAtAsc(post, pageable);

            List<Map<String, Object>> commentList = comments.getContent().stream()
                .map(this::convertCommentToResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("comments", commentList);
            response.put("totalPages", comments.getTotalPages());
            response.put("totalElements", comments.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch comments: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Create comment
    @PostMapping("/posts/{postId}/comments")
    @Transactional
    public ResponseEntity<?> createComment(
            @PathVariable UUID postId,
            @RequestBody CreateCommentRequest request,
            Authentication auth) {
        try {
            System.out.println("=== COMMENT CREATION DEBUG ===");
            System.out.println("PostId: " + postId);
            System.out.println("Request content: " + request.getContent());
            System.out.println("Request parentCommentId: " + request.getParentCommentId());
            
            UUID userId = UUID.fromString(auth.getName());
            System.out.println("UserId: " + userId);
            
            UserEntity user = userRepository.findById(userId).orElseThrow();
            System.out.println("User found: " + user.getName());
            
            PostEntity post = postRepository.findById(postId).orElseThrow();
            System.out.println("Post found: " + post.getContent());

            CommentEntity comment = new CommentEntity(post, user, request.getContent());
            System.out.println("Comment created, saving...");

            if (request.getParentCommentId() != null) {
                CommentEntity parentComment = commentRepository.findById(request.getParentCommentId()).orElseThrow();
                comment.setParentComment(parentComment);
            }

            // Ensure timestamps are set
            if (comment.getCreatedAt() == null) {
                comment.setCreatedAt(Instant.now());
            }
            if (comment.getUpdatedAt() == null) {
                comment.setUpdatedAt(Instant.now());
            }

            comment = commentRepository.save(comment);
            System.out.println("Comment saved with ID: " + comment.getId());
            
            Map<String, Object> response = convertCommentToResponse(comment);
            System.out.println("Comment response: " + response);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in comment creation: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create comment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Like/Unlike post
    @PostMapping("/posts/{postId}/like")
    @Transactional
    public ResponseEntity<?> togglePostLike(@PathVariable UUID postId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            PostEntity post = postRepository.findById(postId).orElseThrow();

            Optional<PostLikeEntity> existingLike = postLikeRepository.findByPostAndUser(post, user);

            if (existingLike.isPresent()) {
                postLikeRepository.delete(existingLike.get());
                Map<String, Object> response = new HashMap<>();
                response.put("liked", false);
                response.put("likeCount", postLikeRepository.countByPost(post));
                return ResponseEntity.ok(response);
            } else {
                PostLikeEntity like = new PostLikeEntity(post, user);
                postLikeRepository.save(like);
                Map<String, Object> response = new HashMap<>();
                response.put("liked", true);
                response.put("likeCount", postLikeRepository.countByPost(post));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to toggle like: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Like/Unlike comment
    @PostMapping("/comments/{commentId}/like")
    @Transactional
    public ResponseEntity<?> toggleCommentLike(@PathVariable UUID commentId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CommentEntity comment = commentRepository.findById(commentId).orElseThrow();

            Optional<CommentLikeEntity> existingLike = commentLikeRepository.findByCommentAndUser(comment, user);

            if (existingLike.isPresent()) {
                commentLikeRepository.delete(existingLike.get());
                Map<String, Object> response = new HashMap<>();
                response.put("liked", false);
                response.put("likeCount", commentLikeRepository.countByComment(comment));
                return ResponseEntity.ok(response);
            } else {
                CommentLikeEntity like = new CommentLikeEntity(comment, user);
                commentLikeRepository.save(like);
                Map<String, Object> response = new HashMap<>();
                response.put("liked", true);
                response.put("likeCount", commentLikeRepository.countByComment(comment));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to toggle like: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Helper methods
    private Map<String, Object> convertPostToResponse(PostEntity post) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", post.getAuthor().getId().toString());
        author.put("name", post.getAuthor().getName());
        author.put("imageUrl", post.getAuthor().getImageUrl() != null ? post.getAuthor().getImageUrl() : "");

        Map<String, Object> response = new HashMap<>();
        response.put("id", post.getId().toString());
        response.put("type", post.getPostType().toString());
        response.put("title", post.getTitle());
        response.put("content", post.getContent());
        response.put("attachments", post.getAttachments() != null ? post.getAttachments() : "");
        response.put("isAssignment", post.getIsAssignment());
        response.put("isPinned", post.getIsPinned());
        response.put("avgScore", post.getAvgScore());
        response.put("scoreCount", post.getScoreCount());
        response.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : Instant.now().toString());
        response.put("author", author);

        if (post.getModule() != null) {
            Map<String, Object> module = new HashMap<>();
            module.put("id", post.getModule().getId().toString());
            module.put("title", post.getModule().getTitle());
            response.put("module", module);
        }

        // Add class information
        if (post.getClassEntity() != null) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", post.getClassEntity().getId().toString());
            classInfo.put("name", post.getClassEntity().getName());
            response.put("class", classInfo);
        }

        // Add like count and comment count
        response.put("likeCount", postLikeRepository.countByPost(post));
        response.put("commentCount", commentRepository.countByPost(post));

        // Add comments (only top-level comments, replies will be nested)
        List<CommentEntity> topLevelComments = commentRepository.findByPostAndParentCommentIsNullOrderByCreatedAtAsc(post);
        List<Map<String, Object>> commentResponses = topLevelComments.stream()
                .map(this::convertCommentToResponse)
                .collect(Collectors.toList());
        response.put("comments", commentResponses);

        return response;
    }

    private Map<String, Object> convertCommentToResponse(CommentEntity comment) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", comment.getAuthor().getId().toString());
        author.put("name", comment.getAuthor().getName());
        author.put("imageUrl", comment.getAuthor().getImageUrl() != null ? comment.getAuthor().getImageUrl() : "");

        Map<String, Object> response = new HashMap<>();
        response.put("id", comment.getId().toString());
        response.put("content", comment.getContent());
        response.put("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : Instant.now().toString());
        response.put("author", author);

        if (comment.getParentComment() != null) {
            response.put("parentCommentId", comment.getParentComment().getId().toString());
        }

        // Add like count
        response.put("likeCount", commentLikeRepository.countByComment(comment));

        // Add replies (only for top-level comments)
        if (comment.getParentComment() == null) {
            List<CommentEntity> replies = commentRepository.findByParentCommentOrderByCreatedAtAsc(comment);
            List<Map<String, Object>> replyResponses = replies.stream()
                    .map(this::convertCommentToResponse)
                    .collect(Collectors.toList());
            response.put("replies", replyResponses);
        } else {
            response.put("replies", new ArrayList<>());
        }

        return response;
    }

    // Rate assignment (only for assignment posts)
    @PostMapping("/posts/{postId}/rate")
    @Transactional
    public ResponseEntity<?> rateAssignment(
            @PathVariable UUID postId,
            @RequestParam Integer score,
            Authentication auth) {
        try {
            System.out.println("=== RATING DEBUG ===");
            System.out.println("PostId: " + postId);
            System.out.println("Score: " + score);
            
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            PostEntity post = postRepository.findById(postId).orElseThrow();

            // Only allow rating assignment posts
            if (post.getPostType() != PostEntity.PostType.ASSIGNMENT) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Only assignment posts can be rated");
                return ResponseEntity.badRequest().body(error);
            }

            // Prevent self-rating
            if (post.getAuthor().getId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot rate your own assignment");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate score range (0-10)
            if (score < 0 || score > 10) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Score must be between 0 and 10");
                return ResponseEntity.badRequest().body(error);
            }

            // Find existing rating or create new one
            Optional<PostLikeEntity> existingRating = postLikeRepository.findByPostAndUser(post, user);
            PostLikeEntity rating;

            if (existingRating.isPresent()) {
                // PREVENT RATING CHANGES - once a student rates, they cannot change it
                if (existingRating.get().getRating() != null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "You have already rated this assignment. Ratings cannot be changed.");
                    error.put("existingRating", existingRating.get().getRating());
                    System.out.println("User attempted to change rating from " + existingRating.get().getRating() + " to " + score);
                    return ResponseEntity.badRequest().body(error);
                }
                rating = existingRating.get();
                rating.setRating(score);
                System.out.println("Setting rating for existing like to: " + score);
            } else {
                rating = new PostLikeEntity(post, user, score);
                System.out.println("Creating new rating: " + score);
            }

            postLikeRepository.save(rating);

            // Calculate average score from all ratings for this post
            List<PostLikeEntity> allRatings = postLikeRepository.findByPost(post);
            List<PostLikeEntity> ratingsWithScores = allRatings.stream()
                    .filter(r -> r.getRating() != null)
                    .collect(Collectors.toList());
            
            BigDecimal avgScore;
            if (!ratingsWithScores.isEmpty()) {
                double average = ratingsWithScores.stream()
                        .mapToInt(PostLikeEntity::getRating)
                        .average()
                        .orElse(0.0);
                
                avgScore = BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
                post.setAvgScore(avgScore);
                post.setScoreCount(ratingsWithScores.size());
                System.out.println("Calculated average score: " + avgScore + " from " + ratingsWithScores.size() + " ratings");
            } else {
                avgScore = BigDecimal.ZERO;
                post.setAvgScore(avgScore);
                post.setScoreCount(0);
                System.out.println("No ratings found, setting to zero");
            }

            postRepository.save(post);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment rated successfully");
            response.put("score", score);
            response.put("avgScore", avgScore);
            response.put("scoreCount", post.getScoreCount());
            
            System.out.println("Response: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in rating: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to rate assignment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get user's rating for an assignment
    @GetMapping("/posts/{postId}/my-rating")
    public ResponseEntity<?> getMyRating(@PathVariable UUID postId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            PostEntity post = postRepository.findById(postId).orElseThrow();

            Optional<PostLikeEntity> rating = postLikeRepository.findByPostAndUser(post, user);

            Map<String, Object> response = new HashMap<>();
            if (rating.isPresent() && rating.get().getRating() != null) {
                response.put("score", rating.get().getRating());
                response.put("hasRated", true);
            } else {
                response.put("score", null);
                response.put("hasRated", false);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get rating: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Update comment
    @PutMapping("/comments/{commentId}")
    @Transactional
    public ResponseEntity<?> updateComment(
            @PathVariable UUID commentId,
            @RequestBody Map<String, String> request,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CommentEntity comment = commentRepository.findById(commentId).orElseThrow();

            // Check if user can edit this comment (only owner can edit)
            boolean isOwner = comment.getAuthor().getId().equals(userId);

            if (!isOwner) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "You can only edit your own comments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            String newContent = request.get("content");
            if (newContent == null || newContent.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Comment content cannot be empty");
                return ResponseEntity.badRequest().body(error);
            }

            comment.setContent(newContent.trim());
            comment.setUpdatedAt(Instant.now());
            comment = commentRepository.save(comment);

            Map<String, Object> response = convertCommentToResponse(comment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update comment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Delete comment
    @DeleteMapping("/comments/{commentId}")
    @Transactional
    public ResponseEntity<?> deleteComment(
            @PathVariable UUID commentId,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CommentEntity comment = commentRepository.findById(commentId).orElseThrow();

            // Check if user can delete this comment (owner or admin)
            boolean isOwner = comment.getAuthor().getId().equals(userId);
            boolean isAdmin = "ADMIN".equals(user.getRole());

            if (!isOwner && !isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "You can only delete your own comments");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Delete all replies to this comment first
            List<CommentEntity> replies = commentRepository.findByParentCommentOrderByCreatedAtAsc(comment);
            commentRepository.deleteAll(replies);

            // Delete the comment itself
            commentRepository.delete(comment);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Comment deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete comment: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Update post
    @PutMapping("/posts/{postId}")
    @Transactional
    public ResponseEntity<?> updatePost(
            @PathVariable UUID postId,
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            PostEntity post = postRepository.findById(postId).orElseThrow();

            // Check if user can edit this post (only owner can edit)
            boolean isOwner = post.getAuthor().getId().equals(userId);

            if (!isOwner) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "You can only edit your own posts");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            String newTitle = (String) request.get("title");
            String newContent = (String) request.get("content");
            String newPostType = (String) request.get("postType");
            Boolean newIsAssignment = (Boolean) request.get("isAssignment");

            if (newTitle != null) {
                post.setTitle(newTitle.trim());
            }
            if (newContent != null) {
                post.setContent(newContent.trim());
            }
            if (newPostType != null) {
                post.setPostType(PostEntity.PostType.valueOf(newPostType));
            }
            if (newIsAssignment != null) {
                post.setIsAssignment(newIsAssignment);
            }

            post.setUpdatedAt(Instant.now());
            post = postRepository.save(post);

            Map<String, Object> response = convertPostToResponse(post);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update post: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Delete post
    @DeleteMapping("/posts/{postId}")
    @Transactional
    public ResponseEntity<?> deletePost(
            @PathVariable UUID postId,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            PostEntity post = postRepository.findById(postId).orElseThrow();

            // Check if user can delete this post (owner or admin)
            boolean isOwner = post.getAuthor().getId().equals(userId);
            boolean isAdmin = "ADMIN".equals(user.getRole());

            if (!isOwner && !isAdmin) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "You can only delete your own posts");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // Delete all comments and replies first
            List<CommentEntity> comments = commentRepository.findByPostOrderByCreatedAtAsc(post);
            commentRepository.deleteAll(comments);

            // Delete all likes
            List<PostLikeEntity> likes = postLikeRepository.findByPost(post);
            postLikeRepository.deleteAll(likes);

            // Delete the post itself
            postRepository.delete(post);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Post deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete post: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // PUBLIC ENDPOINT: Fix existing posts without class_id (temporary fix)
    @GetMapping("/fix-post-classes")
    @Transactional
    public ResponseEntity<?> fixPostClasses() {
        try {
            System.out.println("=== FIXING POST CLASSES ===");
            
            // Get all posts
            List<PostEntity> allPosts = postRepository.findAll();
            System.out.println("Total posts: " + allPosts.size());
            
            int fixed = 0;
            int skipped = 0;
            
            for (PostEntity post : allPosts) {
                // Skip if already has class
                if (post.getClassEntity() != null) {
                    skipped++;
                    continue;
                }
                
                // Get author's enrollments
                List<com.thexperience.backend.entities.ClassEnrollmentEntity> enrollments = 
                    classEnrollmentRepository.findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(post.getAuthor());
                
                if (!enrollments.isEmpty()) {
                    // Assign to first enrollment
                    post.setClassEntity(enrollments.get(0).getClassEntity());
                    postRepository.save(post);
                    System.out.println("Fixed post " + post.getId() + " → assigned to class: " + enrollments.get(0).getClassEntity().getName());
                    fixed++;
                } else {
                    System.out.println("⚠️ Post " + post.getId() + " author has no enrollments");
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Fixed posts successfully");
            response.put("totalPosts", allPosts.size());
            response.put("fixed", fixed);
            response.put("skipped", skipped);
            
            System.out.println("=== FIX COMPLETE ===");
            System.out.println("Total: " + allPosts.size() + ", Fixed: " + fixed + ", Skipped: " + skipped);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in fixing posts: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fix posts: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // DEBUG ENDPOINT: Check posts in database
    @GetMapping("/debug-posts")
    public ResponseEntity<?> debugPosts() {
        try {
            List<PostEntity> allPosts = postRepository.findAll();
            
            List<Map<String, Object>> postsDebug = allPosts.stream()
                .map(post -> {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id", post.getId());
                    p.put("title", post.getTitle());
                    p.put("isAssignment", post.getIsAssignment());
                    p.put("hasClass", post.getClassEntity() != null);
                    p.put("className", post.getClassEntity() != null ? post.getClassEntity().getName() : null);
                    p.put("classId", post.getClassEntity() != null ? post.getClassEntity().getId() : null);
                    p.put("authorName", post.getAuthor().getName());
                    p.put("avgScore", post.getAvgScore());
                    p.put("scoreCount", post.getScoreCount());
                    return p;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "totalPosts", allPosts.size(),
                "posts", postsDebug
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Admin endpoint to toggle post visibility
    @PutMapping("/posts/{postId}/visibility")
    public ResponseEntity<?> togglePostVisibility(@PathVariable UUID postId, @RequestBody Map<String, Boolean> request, Authentication auth) {
        try {
            // Check if user is admin
            UserEntity user = userRepository.findById(UUID.fromString(auth.getName())).orElse(null);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
            }
            
            PostEntity post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return ResponseEntity.notFound().build();
            }
            
            Boolean isHidden = request.get("isHidden");
            if (isHidden != null) {
                post.setIsHidden(isHidden);
                postRepository.save(post);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isHidden", post.getIsHidden(),
                    "message", "Post visibility updated successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "isHidden parameter is required"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
