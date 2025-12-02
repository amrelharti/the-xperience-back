package com.thexperience.backend.admin;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/student-video-activity")
public class StudentVideoActivityController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private VideoAccessLogRepository videoAccessLogRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private TrainingModuleRepository trainingModuleRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Get video activity for all students or specific students (for comparison)
     */
    @GetMapping
    public ResponseEntity<?> getStudentVideoActivity(
            @RequestParam(required = false) List<UUID> studentIds,
            @RequestParam(required = false) UUID classId,
            Authentication auth) {
        try {
            // Check if user is admin
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            // Get students to analyze
            List<UserEntity> students;
            if (studentIds != null && !studentIds.isEmpty()) {
                students = userRepository.findAllById(studentIds);
            } else {
                students = userRepository.findByRole("STUDENT");
            }

            List<Map<String, Object>> studentActivities = new ArrayList<>();

            for (UserEntity student : students) {
                try {
                    Map<String, Object> activity = buildStudentVideoActivity(student, classId);
                    studentActivities.add(activity);
                } catch (Exception e) {
                    System.err.println("Error processing student " + student.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Sort by most videos watched
            studentActivities.sort((a, b) -> 
                Long.compare((Long) b.get("videosWatched"), (Long) a.get("videosWatched"))
            );

            return ResponseEntity.ok(Map.of(
                "students", studentActivities,
                "totalStudents", studentActivities.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch student video activity: " + e.getMessage()
            ));
        }
    }

    private Map<String, Object> buildStudentVideoActivity(UserEntity student, UUID filterClassId) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("id", student.getId());
        activity.put("name", student.getName());
        activity.put("email", student.getEmail());
        activity.put("imageUrl", student.getImageUrl());

        // Get enrolled classes (filter by classId if provided)
        List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
            .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(student);
        
        if (filterClassId != null) {
            enrollments = enrollments.stream()
                .filter(e -> e.getClassEntity().getId().equals(filterClassId))
                .collect(Collectors.toList());
        }

        List<Map<String, Object>> classDetails = new ArrayList<>();
        for (ClassEnrollmentEntity enrollment : enrollments) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("id", enrollment.getClassEntity().getId());
            classInfo.put("name", enrollment.getClassEntity().getName());
            classDetails.add(classInfo);
        }
        activity.put("enrolledClasses", classDetails);

        // Get ALL courses from enrolled classes
        Set<CourseEntity> availableCourses = new HashSet<>();
        Map<UUID, String> courseToClassName = new HashMap<>();
        
        for (ClassEnrollmentEntity enrollment : enrollments) {
            ClassEntity classEntity = enrollment.getClassEntity();
            ModuleEntity training = classEntity.getModule();
            
            // Get all training modules for this training
            List<TrainingModuleEntity> modules = trainingModuleRepository
                .findByTrainingOrderByOrderIndexAsc(training);
            
            for (TrainingModuleEntity module : modules) {
                List<CourseEntity> courses = courseRepository
                    .findByTrainingModuleOrderByOrderIndexAsc(module);
                availableCourses.addAll(courses);
                
                // Map course to class name
                for (CourseEntity course : courses) {
                    courseToClassName.put(course.getId(), classEntity.getName());
                }
            }
        }

        // Get student's progress (only for their enrolled classes)
        List<CourseProgressEntity> allProgress = new ArrayList<>();
        for (ClassEnrollmentEntity enrollment : enrollments) {
            List<CourseProgressEntity> classProgress = courseProgressRepository
                .findByUserAndClassEntity(student, enrollment.getClassEntity());
            allProgress.addAll(classProgress);
        }

        // Build progress map
        Map<UUID, CourseProgressEntity> progressMap = allProgress.stream()
            .collect(Collectors.toMap(
                p -> p.getCourse().getId(),
                p -> p,
                (p1, p2) -> p1.getStartedAt() != null && (p2.getStartedAt() == null || 
                    p1.getStartedAt().isAfter(p2.getStartedAt())) ? p1 : p2
            ));

        // Categorize videos
        List<Map<String, Object>> watchedVideos = new ArrayList<>();
        List<Map<String, Object>> notWatchedVideos = new ArrayList<>();

        for (CourseEntity course : availableCourses) {
            Map<String, Object> videoInfo = createVideoInfo(course, courseToClassName.get(course.getId()));
            CourseProgressEntity progress = progressMap.get(course.getId());

            if (progress != null && progress.isStarted()) {
                // Video was watched
                videoInfo.put("watched", true);
                videoInfo.put("status", progress.getStatus());
                videoInfo.put("watchTimeSeconds", progress.getWatchTimeSeconds() != null ? progress.getWatchTimeSeconds() : 0);
                videoInfo.put("completionPercentage", progress.getCompletionPercentage() != null ? progress.getCompletionPercentage() : 0.0);
                videoInfo.put("startedAt", progress.getStartedAt());
                videoInfo.put("completedAt", progress.getCompletedAt());
                watchedVideos.add(videoInfo);
            } else {
                // Video not watched
                videoInfo.put("watched", false);
                videoInfo.put("status", "NOT_STARTED");
                videoInfo.put("watchTimeSeconds", 0);
                videoInfo.put("completionPercentage", 0.0);
                notWatchedVideos.add(videoInfo);
            }
        }

        // Sort watched videos by most recently started
        watchedVideos.sort((a, b) -> {
            Instant dateA = (Instant) a.get("startedAt");
            Instant dateB = (Instant) b.get("startedAt");
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA);
        });

        // Statistics
        long totalVideosAvailable = availableCourses.size();
        long videosWatched = watchedVideos.size();
        long videosCompleted = (long) watchedVideos.stream()
            .filter(v -> "COMPLETED".equals(v.get("status")))
            .count();
        long totalWatchTime = watchedVideos.stream()
            .mapToLong(v -> (Integer) v.get("watchTimeSeconds"))
            .sum();

        Optional<Instant> lastVideoActivity = watchedVideos.stream()
            .map(v -> (Instant) v.get("startedAt"))
            .filter(Objects::nonNull)
            .max(Instant::compareTo);

        activity.put("totalVideosAvailable", totalVideosAvailable);
        activity.put("videosWatched", videosWatched);
        activity.put("videosCompleted", videosCompleted);
        activity.put("videosNotWatched", totalVideosAvailable - videosWatched);
        activity.put("totalWatchTimeMinutes", Math.round(totalWatchTime / 60.0));
        activity.put("watchTimeHours", Math.round(totalWatchTime / 3600.0 * 100.0) / 100.0);
        activity.put("lastVideoActivity", lastVideoActivity.orElse(null));
        activity.put("watchedVideos", watchedVideos);
        activity.put("notWatchedVideos", notWatchedVideos);

        return activity;
    }

    private Map<String, Object> createVideoInfo(CourseEntity course, String className) {
        Map<String, Object> videoInfo = new HashMap<>();
        videoInfo.put("courseId", course.getId());
        videoInfo.put("title", course.getTitle());
        videoInfo.put("videoUrl", course.getVideoUrl());
        videoInfo.put("durationSeconds", course.getDurationSeconds());
        videoInfo.put("durationMinutes", Math.round(course.getDurationSeconds() / 60.0));
        videoInfo.put("className", className);
        videoInfo.put("orderIndex", course.getOrderIndex());

        if (course.getTrainingModule() != null) {
            videoInfo.put("moduleId", course.getTrainingModule().getId());
            videoInfo.put("moduleName", course.getTrainingModule().getTitle());
            videoInfo.put("moduleOrder", course.getTrainingModule().getOrderIndex());
            
            if (course.getTrainingModule().getTraining() != null) {
                videoInfo.put("trainingId", course.getTrainingModule().getTraining().getId());
                videoInfo.put("trainingName", course.getTrainingModule().getTraining().getTitle());
            }
        }

        return videoInfo;
    }

    /**
     * Compare multiple students side-by-side
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareStudents(
            @RequestParam List<UUID> studentIds,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            if (studentIds.size() > 5) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maximum 5 students can be compared at once"
                ));
            }

            List<UserEntity> students = userRepository.findAllById(studentIds);
            List<Map<String, Object>> comparison = new ArrayList<>();

            for (UserEntity student : students) {
                Map<String, Object> activity = buildStudentVideoActivity(student, null);
                comparison.add(activity);
            }

            return ResponseEntity.ok(Map.of(
                "students", comparison,
                "comparisonCount", comparison.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to compare students: " + e.getMessage()
            ));
        }
    }
}

