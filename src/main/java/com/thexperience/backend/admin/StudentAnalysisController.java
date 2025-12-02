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
@RequestMapping("/api/admin/student-analysis")
public class StudentAnalysisController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private TrainingModuleRepository trainingModuleRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Get comprehensive student profiles (360° view)
     */
    @GetMapping("/students")
    public ResponseEntity<?> getStudentProfiles(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID trainingId,
            Authentication auth) {
        try {
            System.out.println("=== STUDENT ANALYSIS REQUEST ===");
            System.out.println("ClassId filter: " + classId);
            System.out.println("TrainingId filter: " + trainingId);
            
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                System.out.println("Access denied - user is not admin");
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            List<UserEntity> students = userRepository.findByRole("STUDENT");
            System.out.println("Found " + students.size() + " total students");
            
            List<Map<String, Object>> studentProfiles = new ArrayList<>();

            for (UserEntity student : students) {
                try {
                    Map<String, Object> profile = buildStudentProfile(student, classId, trainingId);
                    
                    // Filter out students not in selected class/training if filters applied
                    if (classId != null || trainingId != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> enrollments = (List<Map<String, Object>>) profile.get("enrollments");
                        if (enrollments == null || enrollments.isEmpty()) {
                            System.out.println("Skipping student " + student.getName() + " - no matching enrollments");
                            continue; // Skip this student if no matching enrollments
                        }
                    }
                    
                    studentProfiles.add(profile);
                    System.out.println("Added profile for: " + student.getName());
                } catch (Exception e) {
                    System.err.println("Error building profile for " + student.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Returning " + studentProfiles.size() + " student profiles");

            // Sort by total points (highest first)
            studentProfiles.sort((a, b) -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> statsA = (Map<String, Object>) a.get("leaderboardStats");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> statsB = (Map<String, Object>) b.get("leaderboardStats");
                    
                    Double pointsA = statsA != null ? (Double) statsA.get("totalPoints") : 0.0;
                    Double pointsB = statsB != null ? (Double) statsB.get("totalPoints") : 0.0;
                    
                    return Double.compare(pointsB, pointsA);
                } catch (Exception e) {
                    return 0;
                }
            });

            return ResponseEntity.ok(Map.of(
                "students", studentProfiles,
                "totalStudents", studentProfiles.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch student profiles: " + e.getMessage()
            ));
        }
    }

    /**
     * Build complete profile for a student
     */
    private Map<String, Object> buildStudentProfile(UserEntity student, UUID filterClassId, UUID filterTrainingId) {
        Map<String, Object> profile = new HashMap<>();
        
        // Basic info
        profile.put("id", student.getId());
        profile.put("name", student.getName());
        profile.put("email", student.getEmail());
        profile.put("imageUrl", student.getImageUrl());
        profile.put("createdAt", student.getCreatedAt());

        // Get all enrollments
        List<ClassEnrollmentEntity> allEnrollments = classEnrollmentRepository
            .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(student);

        // Filter enrollments if needed
        List<ClassEnrollmentEntity> enrollments = allEnrollments;
        if (filterClassId != null) {
            enrollments = enrollments.stream()
                .filter(e -> e.getClassEntity() != null && e.getClassEntity().getId().equals(filterClassId))
                .collect(Collectors.toList());
        }
        if (filterTrainingId != null) {
            enrollments = enrollments.stream()
                .filter(e -> e.getClassEntity() != null && 
                            e.getClassEntity().getModule() != null && 
                            e.getClassEntity().getModule().getId().equals(filterTrainingId))
                .collect(Collectors.toList());
        }

        // Build enrollment details
        List<Map<String, Object>> enrollmentDetails = new ArrayList<>();
        for (ClassEnrollmentEntity enrollment : enrollments) {
            try {
                Map<String, Object> enrollmentInfo = new HashMap<>();
                enrollmentInfo.put("classId", enrollment.getClassEntity().getId());
                enrollmentInfo.put("className", enrollment.getClassEntity().getName());
                
                if (enrollment.getClassEntity().getModule() != null) {
                    enrollmentInfo.put("trainingId", enrollment.getClassEntity().getModule().getId());
                    enrollmentInfo.put("trainingName", enrollment.getClassEntity().getModule().getTitle());
                } else {
                    enrollmentInfo.put("trainingId", null);
                    enrollmentInfo.put("trainingName", "N/A");
                }
                
                enrollmentInfo.put("enrolledAt", enrollment.getEnrolledAt());
                enrollmentDetails.add(enrollmentInfo);
            } catch (Exception e) {
                System.err.println("Error processing enrollment for student " + student.getName() + ": " + e.getMessage());
            }
        }
        profile.put("enrollments", enrollmentDetails);

        // Get ALL course progress for this student
        List<CourseProgressEntity> allProgress = new ArrayList<>();
        for (ClassEnrollmentEntity enrollment : enrollments) {
            List<CourseProgressEntity> classProgress = courseProgressRepository
                .findByUserAndClassEntity(student, enrollment.getClassEntity());
            allProgress.addAll(classProgress);
        }

        // Get ALL videos from unlocked modules (not just watched ones)
        Set<UUID> allAvailableCourseIds = new HashSet<>();
        for (ClassEnrollmentEntity enrollment : enrollments) {
            try {
                if (enrollment.getClassEntity() != null && 
                    enrollment.getClassEntity().getModule() != null) {
                    
                    ModuleEntity module = enrollment.getClassEntity().getModule();
                    System.out.println("Processing Training: " + module.getTitle());
                    
                    // Get all training modules (sub-modules) under this training
                    List<TrainingModuleEntity> trainingModules = trainingModuleRepository.findByTrainingOrderByOrderIndexAsc(module);
                    System.out.println("  Found " + trainingModules.size() + " sub-modules");
                    
                    for (TrainingModuleEntity trainingModule : trainingModules) {
                        // Get all courses in this training module
                        List<CourseEntity> moduleCourses = courseRepository.findByTrainingModuleOrderByOrderIndexAsc(trainingModule);
                        System.out.println("  Sub-module: " + trainingModule.getTitle() + " has " + moduleCourses.size() + " courses");
                        
                        for (CourseEntity course : moduleCourses) {
                            allAvailableCourseIds.add(course.getId());
                        }
                    }
                    
                    // Also check for direct courses under the module (if any)
                    List<CourseEntity> directCourses = courseRepository.findByModuleOrderByOrderIndexAsc(module);
                    if (!directCourses.isEmpty()) {
                        System.out.println("  Training has " + directCourses.size() + " direct courses");
                        for (CourseEntity course : directCourses) {
                            allAvailableCourseIds.add(course.getId());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting courses for enrollment: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        long totalVideos = allAvailableCourseIds.size();
        System.out.println("Student " + student.getName() + " has access to " + totalVideos + " total videos");

        // Video watching statistics
        long videosWatched = allProgress.stream().filter(CourseProgressEntity::isStarted).count();
        long videosCompleted = allProgress.stream().filter(CourseProgressEntity::isCompleted).count();
        long videosInProgress = videosWatched - videosCompleted;
        long videosNotStarted = totalVideos - videosWatched;

        // Watch time
        int totalWatchTimeSeconds = allProgress.stream()
            .mapToInt(p -> {
                Integer watchTime = p.getWatchTimeSeconds();
                System.out.println("Course: " + p.getCourse().getTitle() + " - Watch time: " + watchTime);
                return watchTime != null ? watchTime : 0;
            })
            .sum();
        
        System.out.println("Student " + student.getName() + " total watch time: " + totalWatchTimeSeconds + " seconds = " + (totalWatchTimeSeconds / 60.0) + " minutes");

        // Build detailed video list - include ALL available videos
        List<Map<String, Object>> videoDetails = new ArrayList<>();
        
        // Create a map of courseId -> progress for quick lookup
        Map<UUID, CourseProgressEntity> progressMap = new HashMap<>();
        for (CourseProgressEntity progress : allProgress) {
            progressMap.put(progress.getCourse().getId(), progress);
        }
        
        // Get all available courses and build their info
        for (UUID courseId : allAvailableCourseIds) {
            Optional<CourseEntity> courseOpt = courseRepository.findById(courseId);
            if (!courseOpt.isPresent()) continue;
            
            CourseEntity course = courseOpt.get();
            CourseProgressEntity progress = progressMap.get(courseId);
            
            Map<String, Object> videoInfo = new HashMap<>();
            videoInfo.put("courseId", course.getId());
            videoInfo.put("title", course.getTitle());
            
            if (progress != null) {
                // Student has progress on this video
                videoInfo.put("status", progress.getStatus());
                videoInfo.put("watched", progress.isStarted());
                videoInfo.put("completed", progress.isCompleted());
                videoInfo.put("watchTimeSeconds", progress.getWatchTimeSeconds() != null ? progress.getWatchTimeSeconds() : 0);
                videoInfo.put("completionPercentage", progress.getCompletionPercentage() != null ? progress.getCompletionPercentage() : 0.0);
                videoInfo.put("startedAt", progress.getStartedAt());
                videoInfo.put("completedAt", progress.getCompletedAt());
            } else {
                // Student hasn't started this video yet
                videoInfo.put("status", "NOT_STARTED");
                videoInfo.put("watched", false);
                videoInfo.put("completed", false);
                videoInfo.put("watchTimeSeconds", 0);
                videoInfo.put("completionPercentage", 0.0);
                videoInfo.put("startedAt", null);
                videoInfo.put("completedAt", null);
            }
            
            // Add module/training info
            if (course.getTrainingModule() != null) {
                videoInfo.put("moduleId", course.getTrainingModule().getId());
                videoInfo.put("moduleName", course.getTrainingModule().getTitle());
                if (course.getTrainingModule().getTraining() != null) {
                    videoInfo.put("trainingId", course.getTrainingModule().getTraining().getId());
                    videoInfo.put("trainingName", course.getTrainingModule().getTraining().getTitle());
                }
            }
            
            videoDetails.add(videoInfo);
        }

        profile.put("videoStatistics", Map.of(
            "total", totalVideos,
            "watched", videosWatched,
            "completed", videosCompleted,
            "inProgress", videosInProgress,
            "notStarted", videosNotStarted,
            "totalWatchTimeMinutes", Math.round(totalWatchTimeSeconds / 60.0),
            "totalWatchTimeHours", Math.round(totalWatchTimeSeconds / 3600.0 * 100.0) / 100.0
        ));
        profile.put("videos", videoDetails);

        // Calculate points from rated posts (for leaderboard)
        List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthor(student);
        List<PostEntity> scoredPosts = rateablePosts.stream()
            .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
            .collect(Collectors.toList());

        double totalPoints = scoredPosts.stream()
            .mapToDouble(p -> p.getAvgScore().doubleValue())
            .sum();

        int assignmentCount = scoredPosts.size();
        double avgScore = assignmentCount > 0 ? totalPoints / assignmentCount : 0.0;

        profile.put("leaderboardStats", Map.of(
            "totalPoints", Math.round(totalPoints * 100) / 100.0,
            "assignmentCount", assignmentCount,
            "averageScore", Math.round(avgScore * 100) / 100.0
        ));

        // Calculate global rank (rough estimate based on points)
        List<UserEntity> allStudents = userRepository.findByRole("STUDENT");
        List<Double> allPoints = new ArrayList<>();
        for (UserEntity s : allStudents) {
            List<PostEntity> posts = postRepository.findRateablePostsByAuthor(s);
            double points = posts.stream()
                .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                .mapToDouble(p -> p.getAvgScore().doubleValue())
                .sum();
            allPoints.add(points);
        }
        allPoints.sort(Collections.reverseOrder());
        int rank = allPoints.indexOf(totalPoints) + 1;
        if (rank == 0) rank = allStudents.size(); // No points = last

        profile.put("globalRank", rank);
        profile.put("totalStudents", allStudents.size());

        // Last activity
        Optional<Instant> lastActivity = allProgress.stream()
            .map(CourseProgressEntity::getStartedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo);
        profile.put("lastActivity", lastActivity.orElse(null));

        return profile;
    }

    /**
     * Get detailed profile for a single student
     */
    @GetMapping("/students/{studentId}")
    public ResponseEntity<?> getStudentDetailedProfile(
            @PathVariable UUID studentId,
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

            UserEntity student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            Map<String, Object> profile = buildStudentProfile(student, null, null);

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch student profile: " + e.getMessage()
            ));
        }
    }

    /**
     * Get student activity summary (quick stats)
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getStudentSummary(Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            List<UserEntity> students = userRepository.findByRole("STUDENT");
            
            // Quick stats
            int totalStudents = students.size();
            int activeStudents = 0; // Students with activity in last 7 days
            int inactiveStudents = 0;
            int highPerformers = 0; // Students with avg score >= 4.0
            int needsHelp = 0; // Students with avg score < 3.0

            Instant sevenDaysAgo = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);

            for (UserEntity student : students) {
                // Check if active
                List<CourseProgressEntity> progress = courseProgressRepository.findByUser(student);
                Optional<Instant> lastActivity = progress.stream()
                    .map(CourseProgressEntity::getStartedAt)
                    .filter(Objects::nonNull)
                    .max(Instant::compareTo);

                if (lastActivity.isPresent() && lastActivity.get().isAfter(sevenDaysAgo)) {
                    activeStudents++;
                } else {
                    inactiveStudents++;
                }

                // Check performance
                List<PostEntity> scoredPosts = postRepository.findRateablePostsByAuthor(student).stream()
                    .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                    .collect(Collectors.toList());

                if (!scoredPosts.isEmpty()) {
                    double avgScore = scoredPosts.stream()
                        .mapToDouble(p -> p.getAvgScore().doubleValue())
                        .average()
                        .orElse(0.0);

                    if (avgScore >= 4.0) highPerformers++;
                    if (avgScore < 3.0) needsHelp++;
                }
            }

            return ResponseEntity.ok(Map.of(
                "totalStudents", totalStudents,
                "activeStudents", activeStudents,
                "inactiveStudents", inactiveStudents,
                "highPerformers", highPerformers,
                "needsHelp", needsHelp
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch summary: " + e.getMessage()
            ));
        }
    }
}

