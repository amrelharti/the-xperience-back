package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingModuleRepository extends JpaRepository<TrainingModuleEntity, UUID> {
    List<TrainingModuleEntity> findByTrainingOrderByOrderIndexAsc(ModuleEntity training);
    Optional<TrainingModuleEntity> findByTrainingAndOrderIndex(ModuleEntity training, Integer orderIndex);
    Long countByTraining(ModuleEntity training);
}

