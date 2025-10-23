package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID> {
    List<ModuleEntity> findByIsPublishedTrue();
}
