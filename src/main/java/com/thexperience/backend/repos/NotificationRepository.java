package com.thexperience.backend.repos;

import com.thexperience.backend.entities.NotificationEntity;
import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.entities.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    
    // Find notifications by user
    Page<NotificationEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);
    
    // Find unread notifications by user
    Page<NotificationEntity> findByUserAndIsReadFalseOrderByCreatedAtDesc(UserEntity user, Pageable pageable);
    
    // Count unread notifications by user
    long countByUserAndIsReadFalse(UserEntity user);
    
    // Find notification by user and post
    Optional<NotificationEntity> findByUserAndPost(UserEntity user, PostEntity post);
    
    // Check if notification exists for user and post
    boolean existsByUserAndPost(UserEntity user, PostEntity post);
    
    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") UserEntity user);
    
    // Delete all notifications for a user
    void deleteByUser(UserEntity user);
    
    // Delete notifications by user and post
    void deleteByUserAndPost(UserEntity user, PostEntity post);
}

