package com.thexperience.backend.student;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Student-facing endpoints for TrainingModules
 */
@RestController
@RequestMapping("/api/training-modules")
public class StudentTrainingModuleController {
    
    private final TrainingModuleRepository trainingModules;
    private final ModuleRepository trainings;
    private final CourseRepository courses;
    private final ModuleAccessCodeRepository accessCodes;
    private final UserRepository users;
    private final ModuleProgressRepository progress;
    private final com.thexperience.backend.codes.CodeUtils codeUtils;

    public StudentTrainingModuleController(
            TrainingModuleRepository trainingModules,
            ModuleRepository trainings,
            CourseRepository courses,
            ModuleAccessCodeRepository accessCodes,
            UserRepository users,
            ModuleProgressRepository progress,
            com.thexperience.backend.codes.CodeUtils codeUtils) {
        this.trainingModules = trainingModules;
        this.trainings = trainings;
        this.courses = courses;
        this.accessCodes = accessCodes;
        this.users = users;
        this.progress = progress;
        this.codeUtils = codeUtils;
    }

    /**
     * Get module details with courses for a student
     */
    @GetMapping("/{moduleId}")
    public ResponseEntity<?> getModuleDetails(
            @PathVariable UUID moduleId,
            Authentication auth) {
        try {
            TrainingModuleEntity module = trainingModules.findById(moduleId)
                    .orElseThrow(() -> new RuntimeException("Module not found"));

            UUID userId = UUID.fromString(String.valueOf(auth.getName()));
            UserEntity user = users.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get courses for this module
            List<CourseEntity> moduleCourses = module.getCourses();
            System.out.println("🔍 Module: " + module.getTitle() + ", Courses count: " + moduleCourses.size());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", module.getId());
            response.put("trainingId", module.getTraining().getId());
            response.put("title", module.getTitle());
            response.put("description", module.getDescription());
            response.put("thumbnailUrl", module.getThumbnailUrl());
            response.put("orderIndex", module.getOrderIndex());
            response.put("isActive", module.getIsActive());

            // Map courses
            List<Map<String, Object>> coursesData = moduleCourses.stream()
                    .map(course -> {
                        System.out.println("🔍 Course: " + course.getTitle() + " (ID: " + course.getId() + ")");
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("id", course.getId());
                        c.put("title", course.getTitle());
                        c.put("description", course.getDescription());
                        c.put("videoUrl", course.getVideoUrl());
                        c.put("durationSeconds", course.getDurationSeconds());
                        c.put("orderIndex", course.getOrderIndex());
                        return c;
                    })
                    .sorted((a, b) -> Integer.compare((Integer)a.get("orderIndex"), (Integer)b.get("orderIndex")))
                    .toList();

            response.put("courses", coursesData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unlock a module with access code
     */
    @PostMapping("/{moduleId}/unlock")
    public ResponseEntity<?> unlockModule(
            @PathVariable UUID moduleId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String code = body.getOrDefault("code", "");
            if (code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Access code required"));
            }

            TrainingModuleEntity module = trainingModules.findById(moduleId)
                    .orElseThrow(() -> new RuntimeException("Module not found"));

            UUID userId = UUID.fromString(String.valueOf(auth.getName()));
            UserEntity user = users.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the parent training
            ModuleEntity training = module.getTraining();

            // Verify code
            String codeHash = codeUtils.sha256(code);
            ModuleAccessCodeEntity accessCode = accessCodes
                    .findFirstByModuleAndCodeHashAndConsumedAtIsNull(training, codeHash)
                    .orElse(null);

            if (accessCode == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid or already used access code"));
            }

            // Mark code as consumed
            accessCode.setConsumedAt(Instant.now());
            accessCode.setConsumedBy(user);
            accessCodes.save(accessCode);

            // Create or update module progress
            ModuleProgressEntity moduleProgress = progress
                    .findByUserAndModule(user, training)
                    .orElse(new ModuleProgressEntity());

            if (moduleProgress.getUser() == null) {
                moduleProgress.setUser(user);
                moduleProgress.setModule(training);
                moduleProgress.setStartedAt(Instant.now());
            }

            moduleProgress.setStatus("IN_PROGRESS");
            progress.save(moduleProgress);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Module unlocked successfully"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Check if student has access to a module
     */
    @GetMapping("/{moduleId}/access")
    public ResponseEntity<?> checkModuleAccess(
            @PathVariable UUID moduleId,
            Authentication auth) {
        try {
            TrainingModuleEntity module = trainingModules.findById(moduleId)
                    .orElseThrow(() -> new RuntimeException("Module not found"));

            UUID userId = UUID.fromString(String.valueOf(auth.getName()));
            UserEntity user = users.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get the parent training
            ModuleEntity training = module.getTraining();

            // Check if user has consumed any access code for this training
            boolean hasAccess = accessCodes.findByModule(training).stream()
                    .anyMatch(code -> code.getConsumedBy() != null && 
                                      code.getConsumedBy().getId().equals(userId));

            return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

