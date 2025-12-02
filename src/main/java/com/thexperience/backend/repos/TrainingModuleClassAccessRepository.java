package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.TrainingModuleClassAccessEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingModuleClassAccessRepository extends JpaRepository<TrainingModuleClassAccessEntity, UUID> {

    Optional<TrainingModuleClassAccessEntity> findByTrainingModuleAndClassEntity(TrainingModuleEntity trainingModule, ClassEntity classEntity);

    List<TrainingModuleClassAccessEntity> findByTrainingModule(TrainingModuleEntity trainingModule);

    List<TrainingModuleClassAccessEntity> findByTrainingModuleAndClassEntityIn(TrainingModuleEntity trainingModule, List<ClassEntity> classes);
}


