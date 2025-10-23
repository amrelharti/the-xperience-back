package com.thexperience.backend.progress;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CourseProgressController {

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private com.thexperience.backend.repos.TrainingModuleRepository trainingModuleRepository;

    @Autowired
    private com.thexperience.backend.services.ProgressService progressService;

    /**
     * Mark a course as completed
     */
    @PostMapping("/courses/{courseId}/complete")
    public ResponseEntity<?> markCourseAsCompleted(@PathVariable UUID courseId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CourseEntity course = courseRepository.findById(courseId).orElseThrow();

            // Find or create course progress
            Optional<CourseProgressEntity> existingProgress = courseProgressRepository.findByUserAndCourse(user, course);
            
            CourseProgressEntity progress;
            if (existingProgress.isPresent()) {
                progress = existingProgress.get();
            } else {
                progress = new CourseProgressEntity(user, course);
            }

            // Mark as completed
            progress.markAsCompleted();
            progress = courseProgressRepository.save(progress);

            // Calculate updated module progress
            int moduleProgress = progressService.calculateModuleProgress(userId, course.getTrainingModule().getId());

            return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "status", "COMPLETED",
                "completedAt", progress.getCompletedAt(),
                "moduleProgress", moduleProgress,
                "message", "Course marked as completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to mark course as completed: " + e.getMessage()));
        }
    }

    /**
     * Mark a course as started (when user begins watching)
     */
    @PostMapping("/courses/{courseId}/start")
    public ResponseEntity<?> markCourseAsStarted(@PathVariable UUID courseId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CourseEntity course = courseRepository.findById(courseId).orElseThrow();

            // Find or create course progress
            Optional<CourseProgressEntity> existingProgress = courseProgressRepository.findByUserAndCourse(user, course);
            
            CourseProgressEntity progress;
            if (existingProgress.isPresent()) {
                progress = existingProgress.get();
            } else {
                progress = new CourseProgressEntity(user, course);
            }

            // Mark as started
            progress.markAsStarted();
            progress = courseProgressRepository.save(progress);

            return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "status", progress.getStatus(),
                "startedAt", progress.getStartedAt(),
                "message", "Course marked as started"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to mark course as started: " + e.getMessage()));
        }
    }

    /**
     * Update course progress (watch time, completion percentage)
     */
    @PutMapping("/courses/{courseId}/progress")
    public ResponseEntity<?> updateCourseProgress(@PathVariable UUID courseId, @RequestBody ProgressUpdateRequest request, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CourseEntity course = courseRepository.findById(courseId).orElseThrow();

            // Find or create course progress
            Optional<CourseProgressEntity> existingProgress = courseProgressRepository.findByUserAndCourse(user, course);
            
            CourseProgressEntity progress;
            if (existingProgress.isPresent()) {
                progress = existingProgress.get();
            } else {
                progress = new CourseProgressEntity(user, course);
            }

            // Update progress
            progress.markAsStarted();
            progress.setWatchTimeSeconds(request.getWatchTimeSeconds());
            progress.setCompletionPercentage(request.getCompletionPercentage());

            // Auto-complete if watched more than 90%
            if (request.getCompletionPercentage() >= 90.0 && !progress.isCompleted()) {
                progress.markAsCompleted();
            }

            progress = courseProgressRepository.save(progress);

            // Calculate updated module progress
            int moduleProgress = progressService.calculateModuleProgress(userId, course.getTrainingModule().getId());

            return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "status", progress.getStatus(),
                "watchTimeSeconds", progress.getWatchTimeSeconds(),
                "completionPercentage", progress.getCompletionPercentage(),
                "moduleProgress", moduleProgress,
                "message", "Course progress updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update course progress: " + e.getMessage()));
        }
    }

    /**
     * Get course progress for a user
     */
    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<?> getCourseProgress(@PathVariable UUID courseId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            CourseEntity course = courseRepository.findById(courseId).orElseThrow();

            Optional<CourseProgressEntity> progress = courseProgressRepository.findByUserAndCourse(user, course);

            if (progress.isPresent()) {
                CourseProgressEntity courseProgress = progress.get();
                Map<String, Object> response = new HashMap<>();
                response.put("courseId", courseId.toString());
                response.put("status", courseProgress.getStatus());
                response.put("startedAt", courseProgress.getStartedAt());
                response.put("completedAt", courseProgress.getCompletedAt());
                response.put("watchTimeSeconds", courseProgress.getWatchTimeSeconds());
                response.put("completionPercentage", courseProgress.getCompletionPercentage());
                response.put("isCompleted", courseProgress.isCompleted());
                response.put("isStarted", courseProgress.isStarted());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("courseId", courseId.toString());
                response.put("status", "NOT_STARTED");
                response.put("startedAt", null);
                response.put("completedAt", null);
                response.put("watchTimeSeconds", 0);
                response.put("completionPercentage", 0.0);
                response.put("isCompleted", false);
                response.put("isStarted", false);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get course progress: " + e.getMessage()));
        }
    }

    /**
     * Get all course progress for a training module
     */
    @GetMapping("/modules/{moduleId}/courses/progress")
    public ResponseEntity<?> getModuleCourseProgress(@PathVariable UUID moduleId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();

            List<CourseProgressEntity> progressList = courseProgressRepository.findByUserAndCourseTrainingModule(user, trainingModule);
            List<CourseEntity> allCourses = courseRepository.findByTrainingModuleOrderByOrderIndexAsc(trainingModule);

            List<Map<String, Object>> courseProgressList = new ArrayList<>();
            
            for (CourseEntity course : allCourses) {
                Optional<CourseProgressEntity> progress = progressList.stream()
                    .filter(p -> p.getCourse().getId().equals(course.getId()))
                    .findFirst();

                Map<String, Object> courseProgress = new HashMap<>();
                courseProgress.put("courseId", course.getId());
                courseProgress.put("courseTitle", course.getTitle());
                courseProgress.put("courseOrder", course.getOrderIndex());
                
                if (progress.isPresent()) {
                    CourseProgressEntity courseProgressEntity = progress.get();
                    courseProgress.put("status", courseProgressEntity.getStatus());
                    courseProgress.put("startedAt", courseProgressEntity.getStartedAt());
                    courseProgress.put("completedAt", courseProgressEntity.getCompletedAt());
                    courseProgress.put("watchTimeSeconds", courseProgressEntity.getWatchTimeSeconds());
                    courseProgress.put("completionPercentage", courseProgressEntity.getCompletionPercentage());
                    courseProgress.put("isCompleted", courseProgressEntity.isCompleted());
                    courseProgress.put("isStarted", courseProgressEntity.isStarted());
                } else {
                    courseProgress.put("status", "NOT_STARTED");
                    courseProgress.put("startedAt", null);
                    courseProgress.put("completedAt", null);
                    courseProgress.put("watchTimeSeconds", 0);
                    courseProgress.put("completionPercentage", 0.0);
                    courseProgress.put("isCompleted", false);
                    courseProgress.put("isStarted", false);
                }
                
                courseProgressList.add(courseProgress);
            }

            // Calculate module progress
            int moduleProgress = progressService.calculateModuleProgress(userId, moduleId);

            return ResponseEntity.ok(Map.of(
                "moduleId", moduleId,
                "moduleProgress", moduleProgress,
                "courses", courseProgressList
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get module course progress: " + e.getMessage()));
        }
    }

    // Request DTO
    public static class ProgressUpdateRequest {
        private Integer watchTimeSeconds;
        private Double completionPercentage;

        public Integer getWatchTimeSeconds() { return watchTimeSeconds; }
        public void setWatchTimeSeconds(Integer watchTimeSeconds) { this.watchTimeSeconds = watchTimeSeconds; }

        public Double getCompletionPercentage() { return completionPercentage; }
        public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    }
}
