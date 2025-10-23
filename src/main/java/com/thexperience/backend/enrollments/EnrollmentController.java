package com.thexperience.backend.enrollments;

import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.repos.CourseRepository;
import com.thexperience.backend.entities.EnrollmentEntity;
import com.thexperience.backend.repos.EnrollmentRepository;
import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.entities.ModuleProgressEntity;
import com.thexperience.backend.repos.ModuleProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleProgressRepository moduleProgressRepository;

    @GetMapping("/my-courses")
    public ResponseEntity<?> getMyCourses(Authentication authentication) {
        try {
            UUID userId = UUID.fromString(authentication.getName());
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserEntity user = userOpt.get();
            List<EnrollmentEntity> enrollments = enrollmentRepository.findByUser(user);
            
            List<Map<String, Object>> courses = enrollments.stream()
                .map(enrollment -> {
                    CourseEntity course = enrollment.getCourse();
                    // TODO: Fix after restructuring to class-based enrollment
                    List<ModuleEntity> modules = List.of(); // Temporary fix
                    long startedModules = modules.stream()
                        .filter(module -> moduleProgressRepository.findByUserAndModule(user, module)
                            .map(progress -> "IN_PROGRESS".equals(progress.getStatus()) || "COMPLETED".equals(progress.getStatus()))
                            .orElse(false))
                        .count();
                    long completedModules = modules.stream()
                        .filter(module -> moduleProgressRepository.findByUserAndModule(user, module)
                            .map(progress -> "COMPLETED".equals(progress.getStatus()))
                            .orElse(false))
                        .count();
                    int totalModules = modules.size();
                    int percent = totalModules == 0 ? 0 : (int) Math.round((startedModules * 100.0) / totalModules);
                    
                    return Map.of(
                        "id", course.getId().toString(),
                        "title", course.getTitle(),
                        "description", course.getDescription(),
                        "thumbnailUrl", "", // TODO: Fix after restructuring to class-based enrollment
                        "isPublished", true, // TODO: Fix after restructuring to class-based enrollment
                        "progress", Map.of(
                            "startedModules", (int) startedModules,
                            "completedModules", (int) completedModules,
                            "totalModules", totalModules,
                            "percent", percent
                        ),
                        "level", "Beginner", // TODO: Add level to course entity
                        "duration", "2 hours", // TODO: Calculate from modules
                        "instructor", "The Xperience Team", // TODO: Add instructor to course entity
                        "status", startedModules == 0 ? "NOT_STARTED" : 
                                 completedModules == totalModules ? "COMPLETED" : "IN_PROGRESS"
                    );
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get enrolled courses"));
        }
    }

}
