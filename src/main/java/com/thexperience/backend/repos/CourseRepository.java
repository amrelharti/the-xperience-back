package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
    List<CourseEntity> findByModule(ModuleEntity module);
    List<CourseEntity> findByModuleOrderByOrderIndexAsc(ModuleEntity module);
    List<CourseEntity> findByTrainingModule(TrainingModuleEntity trainingModule);
    List<CourseEntity> findByTrainingModuleOrderByOrderIndexAsc(TrainingModuleEntity trainingModule);
}
