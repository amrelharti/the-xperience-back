package com.thexperience.backend.repos;

import com.thexperience.backend.entities.LandingPageSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LandingPageSettingsRepository extends JpaRepository<LandingPageSettingsEntity, UUID> {
    @Query("SELECT s FROM LandingPageSettingsEntity s ORDER BY s.updatedAt DESC")
    Optional<LandingPageSettingsEntity> findFirstByOrderByUpdatedAtDesc();
}

