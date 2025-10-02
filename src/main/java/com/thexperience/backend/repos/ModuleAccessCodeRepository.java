package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ModuleAccessCodeEntity;
import com.thexperience.backend.entities.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleAccessCodeRepository extends JpaRepository<ModuleAccessCodeEntity, UUID> {
    List<ModuleAccessCodeEntity> findByModule(ModuleEntity module);
    Optional<ModuleAccessCodeEntity> findFirstByModuleAndCodeHashAndConsumedAtIsNull(ModuleEntity module, String codeHash);
}
