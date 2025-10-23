package com.thexperience.backend.courses;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/courses")
public class StudentCourseController {
    
    private final CourseAccessCodeRepository courseAccessCodeRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ModuleProgressRepository moduleProgressRepository;
    
    public StudentCourseController(
            CourseAccessCodeRepository courseAccessCodeRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            ModuleProgressRepository moduleProgressRepository) {
        this.courseAccessCodeRepository = courseAccessCodeRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.moduleProgressRepository = moduleProgressRepository;
    }
    
    // Student endpoint to unlock course with code
    @PostMapping("/{courseId}/unlock")
    public ResponseEntity<?> unlockCourse(@PathVariable java.util.UUID courseId,
                                        @RequestBody UnlockCourseRequest request,
                                        Authentication auth) {
        try {
            // Get user ID from authentication
            String userId = auth.getName();
            UserEntity user = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Find the course
            CourseEntity course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Hash the provided code
            String codeHash = hashCode(request.code());
            
            // Find the code
            CourseAccessCodeEntity codeEntity = courseAccessCodeRepository.findByCodeHash(codeHash)
                    .orElseThrow(() -> new RuntimeException("Invalid access code"));
            
            // Check if code is already consumed (if single use)
            if (codeEntity.isSingleUse() && codeEntity.getConsumedAt() != null) {
                return ResponseEntity.badRequest().body("Access code has already been used");
            }
            
            // Check if user already has access to this course
            ModuleProgressEntity existingProgress = moduleProgressRepository.findByUserAndModule(user, course.getModule())
                    .orElse(null);
            
            if (existingProgress != null && "COMPLETED".equals(existingProgress.getStatus())) {
                return ResponseEntity.badRequest().body("You already have access to this course");
            }
            
            // Mark code as consumed
            codeEntity.setConsumedAt(Instant.now());
            codeEntity.setConsumedBy(user);
            courseAccessCodeRepository.save(codeEntity);
            
            // Create or update module progress
            ModuleProgressEntity progress = existingProgress;
            if (progress == null) {
                progress = new ModuleProgressEntity();
                progress.setUser(user);
                progress.setModule(course.getModule());
            }
            progress.setStatus("IN_PROGRESS");
            progress.setStartedAt(Instant.now());
            moduleProgressRepository.save(progress);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Course unlocked successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unlocking course: " + e.getMessage());
        }
    }
    
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing code", e);
        }
    }
    
    public record UnlockCourseRequest(String code) {}
}

