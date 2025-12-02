package com.thexperience.backend.repos;

import com.thexperience.backend.entities.VideoAccessLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoAccessLogRepository extends JpaRepository<VideoAccessLogEntity, UUID> {
    
    long countByVideoId(String videoId);
    
    long countDistinctUserIdByVideoId(String videoId);
    
    List<VideoAccessLogEntity> findByVideoIdOrderByAccessedAtDesc(String videoId);
    
    List<VideoAccessLogEntity> findByUserIdOrderByAccessedAtDesc(UUID userId);
    
    @Query("SELECT v FROM VideoAccessLogEntity v WHERE v.videoId = :videoId AND v.accessedAt >= :since ORDER BY v.accessedAt DESC")
    List<VideoAccessLogEntity> findByVideoIdAndAccessedAtAfterOrderByAccessedAtDesc(String videoId, java.time.Instant since);
    
    @Query("SELECT v FROM VideoAccessLogEntity v WHERE v.userId = :userId AND v.accessedAt >= :since ORDER BY v.accessedAt DESC")
    List<VideoAccessLogEntity> findByUserIdAndAccessedAtAfterOrderByAccessedAtDesc(UUID userId, java.time.Instant since);
    
    // Analytics queries
    long countByAccessedAtAfter(java.time.Instant date);
    long countByUserId(UUID userId);
    List<VideoAccessLogEntity> findByAccessedAtAfter(java.time.Instant date);
}

