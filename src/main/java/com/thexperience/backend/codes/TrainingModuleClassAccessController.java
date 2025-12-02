package com.thexperience.backend.codes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/training-modules")
public class TrainingModuleClassAccessController {

    private final TrainingModuleRepository trainingModules;
    private final ClassRepository classes;
    private final TrainingModuleClassAccessRepository accessRepository;
    private final UserRepository users;

    public TrainingModuleClassAccessController(TrainingModuleRepository trainingModules,
                                               ClassRepository classes,
                                               TrainingModuleClassAccessRepository accessRepository,
                                               UserRepository users) {
        this.trainingModules = trainingModules;
        this.classes = classes;
        this.accessRepository = accessRepository;
        this.users = users;
    }

    @GetMapping("/{trainingModuleId}/class-access")
    public ResponseEntity<?> listAccess(@PathVariable UUID trainingModuleId) {
        TrainingModuleEntity module = trainingModules.findById(trainingModuleId)
                .orElseThrow(() -> new RuntimeException("Training module not found"));

        ModuleEntity training = module.getTraining();
        List<ClassEntity> trainingClasses = classes.findByModuleOrderByStartDateDesc(training);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ClassEntity classEntity : trainingClasses) {
            Optional<TrainingModuleClassAccessEntity> accessOpt = accessRepository.findByTrainingModuleAndClassEntity(module, classEntity);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("classId", classEntity.getId());
            item.put("className", classEntity.getName());
            item.put("startDate", classEntity.getStartDate());
            item.put("endDate", classEntity.getEndDate());
            item.put("isUnlocked", accessOpt.map(TrainingModuleClassAccessEntity::isUnlocked).orElse(false));
            item.put("updatedAt", accessOpt.map(TrainingModuleClassAccessEntity::getUpdatedAt).orElse(null));
            item.put("updatedBy", accessOpt.map(TrainingModuleClassAccessEntity::getUpdatedBy)
                    .map(UserEntity::getName).orElse(null));
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{trainingModuleId}/class-access/{classId}")
    public ResponseEntity<?> updateAccess(
            @PathVariable UUID trainingModuleId,
            @PathVariable UUID classId,
            @RequestBody Map<String, Boolean> body,
            Authentication auth) {

        TrainingModuleEntity module = trainingModules.findById(trainingModuleId)
                .orElseThrow(() -> new RuntimeException("Training module not found"));
        ClassEntity classEntity = classes.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
        UserEntity admin = users.findById(UUID.fromString(auth.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isUnlocked = body.getOrDefault("isUnlocked", false);

        TrainingModuleClassAccessEntity access = accessRepository
                .findByTrainingModuleAndClassEntity(module, classEntity)
                .orElseGet(() -> {
                    TrainingModuleClassAccessEntity entity = new TrainingModuleClassAccessEntity();
                    entity.setTrainingModule(module);
                    entity.setClassEntity(classEntity);
                    return entity;
                });

        access.setUnlocked(isUnlocked);
        access.setUpdatedBy(admin);
        accessRepository.save(access);

        return ResponseEntity.ok(Map.of(
                "classId", classId,
                "isUnlocked", access.isUnlocked(),
                "updatedAt", access.getUpdatedAt(),
                "updatedBy", admin.getName()
        ));
    }
}


