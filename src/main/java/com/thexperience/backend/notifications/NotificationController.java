package com.thexperience.backend.notifications;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    // Get all notifications for current user
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationEntity> notifications = notificationRepository
                    .findByUserOrderByCreatedAtDesc(user, pageable);

            List<Map<String, Object>> notificationList = notifications.getContent().stream()
                    .map(notification -> {
                        Map<String, Object> notifMap = new HashMap<>();
                        notifMap.put("id", notification.getId());
                        notifMap.put("postId", notification.getPost().getId());
                        String actorName = notification.getActor() != null
                                ? notification.getActor().getName()
                                : notification.getPost().getAuthor().getName();
                        notifMap.put("authorName", actorName);
                        notifMap.put("postTitle", notification.getPost().getTitle());
                        notifMap.put("postType", notification.getPost().getPostType().name());
                        notifMap.put("notificationType", notification.getNotificationType().name());
                        notifMap.put("createdAt", notification.getCreatedAt());
                        notifMap.put("read", notification.getIsRead());
                        notifMap.put("link", "/feed/posts/" + notification.getPost().getId());
                        return notifMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notificationList);
            response.put("totalPages", notifications.getTotalPages());
            response.put("totalElements", notifications.getTotalElements());
            response.put("unreadCount", notificationRepository.countByUserAndIsReadFalse(user));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }

    // Get unread count
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch unread count: " + e.getMessage()));
        }
    }

    // Mark notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            NotificationEntity notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Not authorized to modify this notification"));
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);

            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark notification as read: " + e.getMessage()));
        }
    }

    // Mark all notifications as read
    @PutMapping("/mark-all-read")
    @Transactional
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            int updated = notificationRepository.markAllAsReadByUser(user);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read", "updated", updated));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to mark all as read: " + e.getMessage()));
        }
    }

    // Delete notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable UUID id, Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            NotificationEntity notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Not authorized to delete this notification"));
            }

            notificationRepository.delete(notification);
            return ResponseEntity.ok(Map.of("message", "Notification deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete notification: " + e.getMessage()));
        }
    }

    // Clear all notifications
    @DeleteMapping
    public ResponseEntity<?> clearAllNotifications(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            notificationRepository.deleteByUser(user);
            return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear notifications: " + e.getMessage()));
        }
    }

    // Create notifications for users when a post is created
    // This should be called from FeedController when a post is created
    @Transactional
    public void createNotificationsForPost(PostEntity post) {
        try {
            // Get the class and training from the post
            ClassEntity postClass = post.getClassEntity();
            ModuleEntity postTraining = post.getModule(); // This is the training ID

            if (postClass == null || postTraining == null) {
                System.out.println("🔔 Cannot create notifications: post missing class or training");
                return; // Can't create notifications without class and training
            }

            System.out.println("🔔 Creating notifications for post in class: " + postClass.getName() + ", training: " + postTraining.getId());

            // Find all users enrolled in the same class
            List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                    .findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(postClass);

            System.out.println("🔔 Found " + enrollments.size() + " enrollments for class " + postClass.getName());

            // Filter enrollments for the same training
            // The class belongs to a training (module), we need to check if it matches
            List<ClassEnrollmentEntity> relevantEnrollments = enrollments.stream()
                    .filter(enrollment -> {
                        // Check if the class's training matches the post's training
                        ClassEntity classEntity = enrollment.getClassEntity();
                        ModuleEntity classTraining = classEntity.getModule(); // Class's training
                        
                        // Both must have the same training
                        boolean matchesTraining = classTraining != null && 
                                                 postTraining != null && 
                                                 classTraining.getId().equals(postTraining.getId());
                        
                        if (!matchesTraining) {
                            System.out.println("🔔 Enrollment filtered out: training mismatch (class training: " + 
                                             (classTraining != null ? classTraining.getId() : "null") + 
                                             ", post training: " + 
                                             (postTraining != null ? postTraining.getId() : "null") + ")");
                        }
                        return matchesTraining;
                    })
                    .filter(enrollment -> {
                        // Don't notify the post author
                        boolean isAuthor = enrollment.getStudent().getId().equals(post.getAuthor().getId());
                        if (isAuthor) {
                            System.out.println("🔔 Skipping notification for post author: " + enrollment.getStudent().getName());
                        }
                        return !isAuthor;
                    })
                    .collect(Collectors.toList());

            System.out.println("🔔 Creating notifications for " + relevantEnrollments.size() + " users");

            // Create notifications for each relevant user
            int createdCount = 0;
            for (ClassEnrollmentEntity enrollment : relevantEnrollments) {
                UserEntity user = enrollment.getStudent();

                // Check if notification already exists
                if (!notificationRepository.existsByUserAndPost(user, post)) {
                    NotificationEntity notification = new NotificationEntity(user, post, NotificationEntity.NotificationType.POST_CREATED);
                    notification.setActor(post.getAuthor());
                    notificationRepository.save(notification);
                    createdCount++;
                    System.out.println("🔔 Created notification for user: " + user.getName());
                } else {
                    System.out.println("🔔 Notification already exists for user: " + user.getName());
                }
            }

            System.out.println("🔔 Created " + createdCount + " new notifications for post " + post.getId());
        } catch (Exception e) {
            System.err.println("🔔 Error creating notifications for post: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create notification for post author when someone comments on their post
    @Transactional
    public void createNotificationForComment(CommentEntity comment) {
        try {
            PostEntity post = comment.getPost();
            UserEntity postAuthor = post.getAuthor();
            UserEntity commentAuthor = comment.getAuthor();

            // Don't notify if the comment author is the post author (self-comment)
            if (postAuthor.getId().equals(commentAuthor.getId())) {
                System.out.println("🔔 Skipping comment notification: comment author is post author");
                return;
            }

            NotificationEntity notification = new NotificationEntity(
                    postAuthor,
                    post,
                    NotificationEntity.NotificationType.COMMENT,
                    commentAuthor);
            notification.setIsRead(false);
            notificationRepository.save(notification);
            System.out.println("🔔 Created comment notification for post author: " + postAuthor.getName());
        } catch (Exception e) {
            System.err.println("🔔 Error creating comment notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create notification for post author when someone likes their post
    @Transactional
    public void createNotificationForLike(PostEntity post, UserEntity liker) {
        try {
            UserEntity postAuthor = post.getAuthor();

            // Don't notify if the liker is the post author (self-like)
            if (postAuthor.getId().equals(liker.getId())) {
                System.out.println("🔔 Skipping like notification: liker is post author");
                return;
            }

            NotificationEntity notification = new NotificationEntity(
                    postAuthor,
                    post,
                    NotificationEntity.NotificationType.LIKE,
                    liker);
            notification.setIsRead(false);
            notificationRepository.save(notification);
            System.out.println("🔔 Created like notification for post author: " + postAuthor.getName());
        } catch (Exception e) {
            System.err.println("🔔 Error creating like notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Create notification for post author when someone rates their assignment/weekly summary
    @Transactional
    public void createNotificationForRating(PostEntity post, UserEntity rater) {
        try {
            UserEntity postAuthor = post.getAuthor();

            // Don't notify if the rater is the post author (self-rating is already prevented in FeedController)
            if (postAuthor.getId().equals(rater.getId())) {
                System.out.println("🔔 Skipping rating notification: rater is post author");
                return;
            }

            // Only create notifications for ASSIGNMENT and WEEKLY_SUMMARY posts
            if (post.getPostType() != PostEntity.PostType.ASSIGNMENT && 
                post.getPostType() != PostEntity.PostType.WEEKLY_SUMMARY) {
                System.out.println("🔔 Skipping rating notification: post is not rateable");
                return;
            }

            NotificationEntity notification = new NotificationEntity(
                    postAuthor,
                    post,
                    NotificationEntity.NotificationType.RATING,
                    rater);
            notification.setIsRead(false);
            notificationRepository.save(notification);
            System.out.println("🔔 Created rating notification for post author: " + postAuthor.getName());
        } catch (Exception e) {
            System.err.println("🔔 Error creating rating notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

