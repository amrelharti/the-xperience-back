package com.thexperience.backend.chat.repository;

import com.thexperience.backend.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.userId = :userId ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findByUserIdOrderByCreatedAtAsc(@Param("userId") UUID userId);
    
    void deleteByUserId(UUID userId);
}




