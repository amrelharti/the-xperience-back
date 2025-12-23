package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CvScorerSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CvScorerSubmissionRepository extends JpaRepository<CvScorerSubmissionEntity, UUID> {
    List<CvScorerSubmissionEntity> findByEmail(String email);
    List<CvScorerSubmissionEntity> findByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
}

