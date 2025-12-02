package com.thexperience.backend.courses;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CoursesController {
    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final TrainingModuleRepository trainingModules;
    private final ModuleProgressRepository progressRepo;
    private final UserRepository users;
    private final ClassEnrollmentRepository classEnrollments;
    private final TrainingModuleClassAccessRepository classAccessRepository;
    private final ClassRepository classRepository;

    public CoursesController(CourseRepository courses, ModuleRepository modules, TrainingModuleRepository trainingModules,
                             ModuleProgressRepository progressRepo, UserRepository users,
                             ClassEnrollmentRepository classEnrollments,
                             TrainingModuleClassAccessRepository classAccessRepository,
                             ClassRepository classRepository) {
        this.courses = courses;
        this.modules = modules;
        this.trainingModules = trainingModules;
        this.progressRepo = progressRepo;
        this.users = users;
        this.classEnrollments = classEnrollments;
        this.classAccessRepository = classAccessRepository;
        this.classRepository = classRepository;
    }

    @GetMapping("/courses")
    public List<Map<String,Object>> list(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElse(null);
        if (user == null) {
            // Return empty list if user not found
            return List.of();
        }
        return modules.findAll().stream().map(m -> {
            List<CourseEntity> courseList = courses.findByModule(m);
            long completed = courseList.stream().filter(c -> progressRepo.findByUserAndModule(user, m).map(p -> "COMPLETED".equals(p.getStatus())).orElse(false)).count();
            Map<String,Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("title", m.getTitle());
            map.put("description", m.getDescription());
            map.put("thumbnailUrl", m.getThumbnailUrl());
            map.put("isPublished", m.isPublished());
            Map<String,Object> prog = new HashMap<>();
            prog.put("completedModules", completed);
            prog.put("totalModules", courseList.size());
            prog.put("percent", courseList.size() == 0 ? 0 : Math.round((completed * 100.0) / courseList.size()));
            map.put("progress", prog);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/courses/{trainingId}")
    public Map<String,Object> detail(
            @PathVariable UUID trainingId,
            @RequestParam(required = false) UUID classId, // CRITICAL: Get classId from query params
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        ModuleEntity training = modules.findById(trainingId).orElseThrow();
        
        // Get training modules for this training
        List<TrainingModuleEntity> modulesList = trainingModules.findByTrainingOrderByOrderIndexAsc(training);
        
        List<Map<String,Object>> moduleDtos = new ArrayList<>();
        for (TrainingModuleEntity module : modulesList) {
            Map<String,Object> moduleDto = new HashMap<>();
            moduleDto.put("id", module.getId());
            moduleDto.put("title", module.getTitle());
            moduleDto.put("description", module.getDescription());
            moduleDto.put("thumbnailUrl", module.getThumbnailUrl());
            moduleDto.put("orderIndex", module.getOrderIndex());
            moduleDto.put("isActive", module.getIsActive());
            
            // Determine access using class-level toggles
            boolean hasAccess;
            if (classId != null) {
                // If classId is provided, check access for that specific class only
                ClassEntity classEntity = classRepository.findById(classId).orElse(null);
                if (classEntity != null) {
                    Optional<TrainingModuleClassAccessEntity> accessOpt = classAccessRepository
                            .findByTrainingModuleAndClassEntity(module, classEntity);
                    hasAccess = accessOpt.map(TrainingModuleClassAccessEntity::isUnlocked).orElse(false);
                    System.out.println("🔍 Module " + module.getTitle() + " for class " + classEntity.getName() + 
                                     " (ID: " + classId + "): hasAccess=" + hasAccess + 
                                     ", accessOpt present=" + accessOpt.isPresent());
                } else {
                    // Class not found, default to locked
                    System.out.println("⚠️ Class not found: " + classId);
                    hasAccess = false;
                }
            } else {
                // If no classId provided, check all active enrollments (backward compatibility)
                List<ClassEnrollmentEntity> activeEnrollments = classEnrollments.findActiveEnrollmentsByStudentAndModule(user, training);
                hasAccess = activeEnrollments.stream()
                        .map(ClassEnrollmentEntity::getClassEntity)
                        .anyMatch(classEntity -> classAccessRepository
                                .findByTrainingModuleAndClassEntity(module, classEntity)
                                .map(TrainingModuleClassAccessEntity::isUnlocked)
                                .orElse(false));
            }
            
            moduleDto.put("hasAccess", hasAccess);
            moduleDto.put("status", hasAccess ? "UNLOCKED" : "LOCKED");
            
            // Get courses count for this module
            List<CourseEntity> moduleCourses = courses.findByTrainingModule(module);
            moduleDto.put("coursesCount", moduleCourses.size());
            
            moduleDtos.add(moduleDto);
        }
        
        Map<String,Object> out = new HashMap<>();
        out.put("id", training.getId());
        out.put("title", training.getTitle());
        out.put("description", training.getDescription());
        out.put("thumbnailUrl", training.getThumbnailUrl());
        out.put("modules", moduleDtos);
        return out;
    }

    @PostMapping("/courses/{moduleId}/enroll")
    public ResponseEntity<?> enroll(@PathVariable UUID moduleId, Authentication auth) {
        // This endpoint is now handled by the ClassController for class enrollment
        // Students enroll in classes, not directly in modules
        return ResponseEntity.badRequest().body("Please enroll in a class for this module");
    }

    @GetMapping("/modules/{moduleId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable UUID moduleId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        ModuleEntity m = modules.findById(moduleId).orElseThrow();
        ModuleProgressEntity mp = progressRepo.findByUserAndModule(user, m).orElse(null);
        if (mp == null) {
            return ResponseEntity.ok(Map.of("status", "LOCKED"));
        }
        return ResponseEntity.ok(Map.of("status", mp.getStatus()));
    }

    @PostMapping("/modules/{moduleId}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID moduleId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        ModuleEntity m = modules.findById(moduleId).orElseThrow();
        ModuleProgressEntity mp = progressRepo.findByUserAndModule(user, m).orElse(null);
        if (mp == null) { mp = new ModuleProgressEntity(); mp.setUser(user); mp.setModule(m); }
        mp.setStatus("COMPLETED"); mp.setCompletedAt(java.time.Instant.now());
        progressRepo.save(mp);
        return ResponseEntity.ok(Map.of("completed", true));
    }
}
