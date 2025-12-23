package com.thexperience.backend.repos;

import com.thexperience.backend.entities.LandingPageLeadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface LandingPageLeadRepository extends JpaRepository<LandingPageLeadEntity, UUID> {
    List<LandingPageLeadEntity> findByPhone(String phone);
    List<LandingPageLeadEntity> findByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtAfter(LocalDateTime date);
    List<LandingPageLeadEntity> findAllByOrderByCreatedAtDesc();
}

