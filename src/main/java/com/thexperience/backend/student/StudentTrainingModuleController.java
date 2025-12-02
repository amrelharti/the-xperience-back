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
    private final UserRepository users;
    private final ClassEnrollmentRepository classEnrollments;
    private final TrainingModuleClassAccessRepository classAccessRepository;

    public StudentTrainingModuleController(
            TrainingModuleRepository trainingModules,
            ModuleRepository trainings,
            CourseRepository courses,
            UserRepository users,
            ClassEnrollmentRepository classEnrollments,
            TrainingModuleClassAccessRepository classAccessRepository) {
        this.trainingModules = trainingModules;
        this.trainings = trainings;
        this.courses = courses;
        this.users = users;
        this.classEnrollments = classEnrollments;
        this.classAccessRepository = classAccessRepository;
    }

    /**
     * Get all training modules across all trainings
     */
    @GetMapping
    public ResponseEntity<?> getAllTrainingModules() {
        try {
            List<TrainingModuleEntity> allModules = trainingModules.findAll();
            
            List<Map<String, Object>> modulesData = allModules.stream()
                    .filter(module -> module.getIsActive() != null && module.getIsActive())
                    .map(module -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", module.getId());
                        m.put("title", module.getTitle());
                        m.put("description", module.getDescription() != null ? module.getDescription() : "");
                        m.put("trainingId", module.getTraining().getId());
                        m.put("trainingTitle", module.getTraining().getTitle());
                        return m;
                    })
                    .sorted((a, b) -> ((String)a.get("title")).compareTo((String)b.get("title")))
                    .toList();
            
            return ResponseEntity.ok(modulesData);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to retrieve training modules"));
        }
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

            List<ClassEnrollmentEntity> activeEnrollments = classEnrollments.findActiveEnrollmentsByStudentAndModule(user, module.getTraining());
            boolean hasAccess = activeEnrollments.stream()
                    .map(ClassEnrollmentEntity::getClassEntity)
                    .anyMatch(classEntity -> classAccessRepository
                            .findByTrainingModuleAndClassEntity(module, classEntity)
                            .map(TrainingModuleClassAccessEntity::isUnlocked)
                            .orElse(false));
            response.put("hasAccess", hasAccess);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unlock a module with access code
     * NOTE: This endpoint is deprecated. Use /api/admin/training-modules/{id}/codes/verify instead.
     * Kept for backward compatibility.
     */
    @PostMapping("/{moduleId}/unlock")
    public ResponseEntity<?> unlockModule(
            @PathVariable UUID moduleId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            // Redirect to new verification endpoint
            return ResponseEntity.badRequest().body(Map.of(
                "error", "This endpoint is deprecated. Please use the new verification endpoint.",
                "message", "Contact your instructor for the correct access code system."
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

            // Determine access based on class toggles
            List<ClassEnrollmentEntity> activeEnrollments = classEnrollments.findActiveEnrollmentsByStudentAndModule(user, module.getTraining());
            boolean hasAccess = activeEnrollments.stream()
                    .map(ClassEnrollmentEntity::getClassEntity)
                    .anyMatch(classEntity -> classAccessRepository
                            .findByTrainingModuleAndClassEntity(module, classEntity)
                            .map(TrainingModuleClassAccessEntity::isUnlocked)
                            .orElse(false));

            return ResponseEntity.ok(Map.of("hasAccess", hasAccess));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

