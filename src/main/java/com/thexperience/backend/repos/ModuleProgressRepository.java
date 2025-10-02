package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ModuleProgressEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgressEntity, UUID> {
    Optional<ModuleProgressEntity> findByUserAndModule(UserEntity user, ModuleEntity module);
}
