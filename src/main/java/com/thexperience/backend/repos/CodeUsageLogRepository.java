package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CodeUsageLogEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeUsageLogRepository extends JpaRepository<CodeUsageLogEntity, UUID> {
    List<CodeUsageLogEntity> findByCodeIdOrderByUsedAtDesc(UUID codeId);
    List<CodeUsageLogEntity> findByResourceIdOrderByUsedAtDesc(UUID resourceId);
    long countByCodeId(UUID codeId);
    long countByResourceId(UUID resourceId);
    boolean existsByCodeIdAndUser(UUID codeId, UserEntity user);
}




