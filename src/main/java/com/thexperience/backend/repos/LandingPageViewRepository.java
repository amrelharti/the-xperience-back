package com.thexperience.backend.repos;

import com.thexperience.backend.entities.LandingPageViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LandingPageViewRepository extends JpaRepository<LandingPageViewEntity, UUID> {
    long count();
    long countByViewedAtAfter(LocalDateTime date);
}

