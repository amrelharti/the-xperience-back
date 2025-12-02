package com.thexperience.backend.admin;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private VideoAccessLogRepository videoAccessLogRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private TrainingModuleRepository trainingModuleRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    /**
     * Get overall platform analytics
     */
    @GetMapping("/overview")
    public ResponseEntity<?> getOverviewAnalytics(Authentication auth) {
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

            Map<String, Object> analytics = new HashMap<>();

            // User statistics
            long totalUsers = userRepository.count();
            long studentCount = userRepository.countByRole("STUDENT");
            long adminCount = userRepository.countByRole("ADMIN");
            
            // Recent user signups (last 30 days)
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            long recentSignups = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

            // Course statistics
            long totalCourses = courseRepository.count();
            long publishedCourses = totalCourses; // All courses are considered published
            
            // Training modules
            long totalModules = trainingModuleRepository.count();
            long publishedModules = trainingModuleRepository.countByIsActive(true);

            // Progress statistics
            long totalProgressRecords = courseProgressRepository.count();
            long completedCourses = courseProgressRepository.countByStatus("COMPLETED");
            long inProgressCourses = courseProgressRepository.countByStatus("IN_PROGRESS");
            
            // Calculate average completion rate
            double completionRate = totalProgressRecords > 0 
                ? (double) completedCourses / totalProgressRecords * 100 
                : 0;

            // Video access statistics (last 30 days)
            long recentVideoAccesses = videoAccessLogRepository.countByAccessedAtAfter(thirtyDaysAgo);
            long totalVideoAccesses = videoAccessLogRepository.count();

            // Class statistics
            long totalClasses = classRepository.count();
            long activeClasses = classRepository.countByIsActiveTrue();
            long totalEnrollments = classEnrollmentRepository.count();
            long activeEnrollments = classEnrollmentRepository.countByIsActiveTrue();

            // Post statistics
            long totalPosts = postRepository.count();
            long assignmentPosts = postRepository.countByPostType(PostEntity.PostType.ASSIGNMENT);
            long weeklySummaryPosts = postRepository.countByPostType(PostEntity.PostType.WEEKLY_SUMMARY);
            long questionPosts = postRepository.countByPostType(PostEntity.PostType.POST);

            // Average watch time
            Double avgWatchTime = courseProgressRepository.findAverageWatchTimeSeconds();
            if (avgWatchTime == null) avgWatchTime = 0.0;

            analytics.put("users", Map.of(
                "total", totalUsers,
                "students", studentCount,
                "admins", adminCount,
                "recentSignups", recentSignups
            ));

            analytics.put("courses", Map.of(
                "total", totalCourses,
                "published", publishedCourses,
                "totalModules", totalModules,
                "publishedModules", publishedModules
            ));

            analytics.put("progress", Map.of(
                "totalRecords", totalProgressRecords,
                "completed", completedCourses,
                "inProgress", inProgressCourses,
                "completionRate", Math.round(completionRate * 100) / 100.0,
                "avgWatchTimeMinutes", Math.round(avgWatchTime / 60 * 100) / 100.0
            ));

            analytics.put("videos", Map.of(
                "totalAccesses", totalVideoAccesses,
                "recentAccesses", recentVideoAccesses
            ));

            analytics.put("classes", Map.of(
                "total", totalClasses,
                "active", activeClasses,
                "totalEnrollments", totalEnrollments,
                "activeEnrollments", activeEnrollments
            ));

            analytics.put("posts", Map.of(
                "total", totalPosts,
                "assignments", assignmentPosts,
                "weeklySummaries", weeklySummaryPosts,
                "questions", questionPosts
            ));

            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get course-specific analytics
     */
    @GetMapping("/courses")
    public ResponseEntity<?> getCourseAnalytics(Authentication auth) {
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

            List<CourseEntity> allCourses = courseRepository.findAll();
            List<Map<String, Object>> courseAnalytics = new ArrayList<>();

            for (CourseEntity course : allCourses) {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("id", course.getId());
                courseData.put("title", course.getTitle());
                courseData.put("isPublished", true); // CourseEntity doesn't have published status

                // Get progress statistics for this course
                List<CourseProgressEntity> progressList = courseProgressRepository.findByCourse(course);
                long totalEnrolled = progressList.size();
                long completed = progressList.stream().filter(CourseProgressEntity::isCompleted).count();
                long inProgress = progressList.stream().filter(p -> p.isStarted() && !p.isCompleted()).count();
                
                double completionRate = totalEnrolled > 0 ? (double) completed / totalEnrolled * 100 : 0;

                // Average watch time for this course
                Double avgWatchTime = progressList.stream()
                    .mapToDouble(p -> p.getWatchTimeSeconds() != null ? p.getWatchTimeSeconds() : 0)
                    .average()
                    .orElse(0.0);

                courseData.put("totalEnrolled", totalEnrolled);
                courseData.put("completed", completed);
                courseData.put("inProgress", inProgress);
                courseData.put("completionRate", Math.round(completionRate * 100) / 100.0);
                courseData.put("avgWatchTimeMinutes", Math.round(avgWatchTime / 60 * 100) / 100.0);

                courseAnalytics.add(courseData);
            }

            // Sort by most enrolled
            courseAnalytics.sort((a, b) -> 
                Long.compare((Long) b.get("totalEnrolled"), (Long) a.get("totalEnrolled"))
            );

            return ResponseEntity.ok(Map.of(
                "courses", courseAnalytics
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch course analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get student engagement analytics
     */
    @GetMapping("/engagement")
    public ResponseEntity<?> getEngagementAnalytics(Authentication auth) {
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

            List<UserEntity> students = userRepository.findByRole("STUDENT");
            List<Map<String, Object>> studentEngagement = new ArrayList<>();

            for (UserEntity student : students) {
                try {
                Map<String, Object> engagement = new HashMap<>();
                engagement.put("id", student.getId());
                engagement.put("name", student.getName());
                engagement.put("email", student.getEmail());

                // Count courses started and completed
                List<CourseProgressEntity> progress = courseProgressRepository.findByUser(student);
                long coursesStarted = progress.stream().filter(CourseProgressEntity::isStarted).count();
                long coursesCompleted = progress.stream().filter(CourseProgressEntity::isCompleted).count();

                // Total watch time
                int totalWatchTime = progress.stream()
                    .mapToInt(p -> p.getWatchTimeSeconds() != null ? p.getWatchTimeSeconds() : 0)
                    .sum();

                // Posts created
                long totalPosts = postRepository.countByAuthor(student);
                
                // Count assignments and summaries using existing methods
                List<PostEntity> allPosts = postRepository.findByAuthor(student);
                long assignments = allPosts.stream()
                    .filter(p -> p.getPostType() == PostEntity.PostType.ASSIGNMENT)
                    .count();
                long weeklySummaries = allPosts.stream()
                    .filter(p -> p.getPostType() == PostEntity.PostType.WEEKLY_SUMMARY)
                    .count();

                // Video accesses
                long videoAccesses = videoAccessLogRepository.countByUserId(student.getId());

                // Classes enrolled
                long classesEnrolled = classEnrollmentRepository.countByStudentAndIsActiveTrue(student);

                // Calculate engagement score (custom formula)
                double engagementScore = (coursesCompleted * 10) + 
                                       (coursesStarted * 5) + 
                                       (assignments * 8) + 
                                       (weeklySummaries * 6) +
                                       (totalPosts * 2) +
                                       (videoAccesses * 0.5);

                engagement.put("coursesStarted", coursesStarted);
                engagement.put("coursesCompleted", coursesCompleted);
                engagement.put("totalWatchTimeMinutes", Math.round(totalWatchTime / 60.0));
                engagement.put("totalPosts", totalPosts);
                engagement.put("assignments", assignments);
                engagement.put("weeklySummaries", weeklySummaries);
                engagement.put("videoAccesses", videoAccesses);
                engagement.put("classesEnrolled", classesEnrolled);
                engagement.put("engagementScore", Math.round(engagementScore));

                studentEngagement.add(engagement);
                } catch (Exception e) {
                    System.err.println("Error processing student " + student.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with next student
                }
            }

            // Sort by engagement score
            studentEngagement.sort((a, b) -> 
                Integer.compare((Integer) b.get("engagementScore"), (Integer) a.get("engagementScore"))
            );

            return ResponseEntity.ok(Map.of(
                "students", studentEngagement.stream().limit(50).collect(Collectors.toList())
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch engagement analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get time-based activity trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getActivityTrends(
            @RequestParam(defaultValue = "30") int days,
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

            Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);

            // User signups over time
            List<UserEntity> recentUsers = userRepository.findByCreatedAtAfter(startDate);
            Map<String, Long> signupsByDate = recentUsers.stream()
                .collect(Collectors.groupingBy(
                    u -> LocalDate.ofInstant(u.getCreatedAt(), ZoneId.systemDefault()).toString(),
                    Collectors.counting()
                ));

            // Video accesses over time
            List<VideoAccessLogEntity> recentAccesses = videoAccessLogRepository.findByAccessedAtAfter(startDate);
            Map<String, Long> accessesByDate = recentAccesses.stream()
                .collect(Collectors.groupingBy(
                    v -> LocalDate.ofInstant(v.getAccessedAt(), ZoneId.systemDefault()).toString(),
                    Collectors.counting()
                ));

            // Course completions over time
            List<CourseProgressEntity> recentCompletions = courseProgressRepository.findByCompletedAtAfter(startDate);
            Map<String, Long> completionsByDate = recentCompletions.stream()
                .filter(p -> p.getCompletedAt() != null)
                .collect(Collectors.groupingBy(
                    p -> LocalDate.ofInstant(p.getCompletedAt(), ZoneId.systemDefault()).toString(),
                    Collectors.counting()
                ));

            // Posts over time
            List<PostEntity> recentPosts = postRepository.findByCreatedAtAfter(startDate);
            Map<String, Long> postsByDate = recentPosts.stream()
                .collect(Collectors.groupingBy(
                    p -> LocalDate.ofInstant(p.getCreatedAt(), ZoneId.systemDefault()).toString(),
                    Collectors.counting()
                ));

            return ResponseEntity.ok(Map.of(
                "signupsByDate", signupsByDate,
                "videoAccessesByDate", accessesByDate,
                "completionsByDate", completionsByDate,
                "postsByDate", postsByDate,
                "period", days + " days"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch trends: " + e.getMessage()
            ));
        }
    }

    /**
     * Get class-specific analytics
     */
    @GetMapping("/classes")
    public ResponseEntity<?> getClassAnalytics(Authentication auth) {
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

            List<ClassEntity> allClasses = classRepository.findAll();
            List<Map<String, Object>> classAnalytics = new ArrayList<>();

            for (ClassEntity classEntity : allClasses) {
                Map<String, Object> classData = new HashMap<>();
                classData.put("id", classEntity.getId());
                classData.put("name", classEntity.getName());
                classData.put("code", ""); // Classes don't have a direct code field
                classData.put("isActive", classEntity.getIsActive());

                // Get enrollments
                List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                    .findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classEntity);
                
                long totalEnrolled = enrollments.size();
                
                // Get progress for students in this class
                List<UserEntity> students = enrollments.stream()
                    .map(ClassEnrollmentEntity::getStudent)
                    .collect(Collectors.toList());

                long totalProgress = 0;
                long totalCompleted = 0;
                int totalWatchTime = 0;
                long totalPosts = 0;

                for (UserEntity student : students) {
                    List<CourseProgressEntity> studentProgress = courseProgressRepository
                        .findByUserAndClassEntity(student, classEntity);
                    totalProgress += studentProgress.size();
                    totalCompleted += studentProgress.stream().filter(CourseProgressEntity::isCompleted).count();
                    totalWatchTime += studentProgress.stream()
                        .mapToInt(p -> p.getWatchTimeSeconds() != null ? p.getWatchTimeSeconds() : 0)
                        .sum();

                    totalPosts += postRepository.countByAuthorAndClassEntity(student, classEntity);
                }

                double avgCompletionRate = totalProgress > 0 ? (double) totalCompleted / totalProgress * 100 : 0;
                double avgWatchTime = totalEnrolled > 0 ? (double) totalWatchTime / totalEnrolled / 60 : 0;

                classData.put("totalEnrolled", totalEnrolled);
                classData.put("totalProgress", totalProgress);
                classData.put("totalCompleted", totalCompleted);
                classData.put("avgCompletionRate", Math.round(avgCompletionRate * 100) / 100.0);
                classData.put("avgWatchTimeMinutes", Math.round(avgWatchTime * 100) / 100.0);
                classData.put("totalPosts", totalPosts);

                classAnalytics.add(classData);
            }

            // Sort by most enrolled
            classAnalytics.sort((a, b) -> 
                Long.compare((Long) b.get("totalEnrolled"), (Long) a.get("totalEnrolled"))
            );

            return ResponseEntity.ok(Map.of(
                "classes", classAnalytics
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch class analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * PHASE 1: Get class rosters with student progress
     */
    @GetMapping("/class-rosters")
    public ResponseEntity<?> getClassRosters(
            @RequestParam(required = false) UUID classId,
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

            List<ClassEntity> classes = classId != null 
                ? List.of(classRepository.findById(classId).orElseThrow())
                : classRepository.findAll();

            List<Map<String, Object>> rosters = new ArrayList<>();

            for (ClassEntity classEntity : classes) {
                Map<String, Object> roster = new HashMap<>();
                roster.put("classId", classEntity.getId());
                roster.put("className", classEntity.getName());
                roster.put("startDate", classEntity.getStartDate());
                roster.put("endDate", classEntity.getEndDate());
                roster.put("isActive", classEntity.getIsActive());

                List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                    .findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classEntity);

                List<Map<String, Object>> students = new ArrayList<>();
                for (ClassEnrollmentEntity enrollment : enrollments) {
                    UserEntity student = enrollment.getStudent();
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("id", student.getId());
                    studentInfo.put("name", student.getName());
                    studentInfo.put("email", student.getEmail());
                    studentInfo.put("enrolledAt", enrollment.getEnrolledAt());

                    // Get progress
                    List<CourseProgressEntity> progress = courseProgressRepository
                        .findByUserAndClassEntity(student, classEntity);
                    long completed = progress.stream().filter(CourseProgressEntity::isCompleted).count();
                    long total = progress.size();
                    double completionRate = total > 0 ? (double) completed / total * 100 : 0;

                    studentInfo.put("coursesTotal", total);
                    studentInfo.put("coursesCompleted", completed);
                    studentInfo.put("completionRate", Math.round(completionRate * 100) / 100.0);

                    // Last activity
                    Optional<CourseProgressEntity> lastActivity = progress.stream()
                        .filter(p -> p.getStartedAt() != null)
                        .max((a, b) -> a.getStartedAt().compareTo(b.getStartedAt()));
                    studentInfo.put("lastActivity", lastActivity.map(CourseProgressEntity::getStartedAt).orElse(null));

                    students.add(studentInfo);
                }

                roster.put("students", students);
                roster.put("totalStudents", students.size());
                rosters.add(roster);
            }

            return ResponseEntity.ok(Map.of("rosters", rosters));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch class rosters: " + e.getMessage()
            ));
        }
    }

    /**
     * PHASE 1: Get completion tracker (modules and trainings)
     */
    @GetMapping("/completions")
    public ResponseEntity<?> getCompletionTracker(Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            // Get all training modules
            List<TrainingModuleEntity> modules = trainingModuleRepository.findAll();
            List<Map<String, Object>> completionData = new ArrayList<>();

            for (TrainingModuleEntity module : modules) {
                Map<String, Object> moduleData = new HashMap<>();
                moduleData.put("moduleId", module.getId());
                moduleData.put("moduleName", module.getTitle());
                moduleData.put("trainingName", module.getTraining().getTitle());

                // Get all courses in this module
                List<CourseEntity> courses = courseRepository.findByTrainingModuleOrderByOrderIndexAsc(module);
                int totalCourses = courses.size();

                // Find students who completed ALL courses in this module
                List<UserEntity> students = userRepository.findByRole("STUDENT");
                List<Map<String, Object>> completedStudents = new ArrayList<>();

                for (UserEntity student : students) {
                    // Check all classes this student is in
                    List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                        .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(student);

                    for (ClassEnrollmentEntity enrollment : enrollments) {
                        long completedInModule = courseProgressRepository
                            .countCompletedCoursesByUserAndTrainingModuleAndClassEntity(
                                student, module, enrollment.getClassEntity()
                            );

                        if (completedInModule == totalCourses && totalCourses > 0) {
                            Map<String, Object> studentInfo = new HashMap<>();
                            studentInfo.put("id", student.getId());
                            studentInfo.put("name", student.getName());
                            studentInfo.put("email", student.getEmail());
                            studentInfo.put("className", enrollment.getClassEntity().getName());
                            studentInfo.put("completedCourses", completedInModule);

                            // Get completion date (last course completed)
                            List<CourseProgressEntity> moduleProgress = courseProgressRepository
                                .findByUserAndCourseTrainingModuleAndClassEntity(
                                    student, module, enrollment.getClassEntity()
                                );
                            Optional<Instant> lastCompletion = moduleProgress.stream()
                                .filter(CourseProgressEntity::isCompleted)
                                .map(CourseProgressEntity::getCompletedAt)
                                .max(Instant::compareTo);

                            studentInfo.put("completedAt", lastCompletion.orElse(null));
                            completedStudents.add(studentInfo);
                            break; // Only count once per student
                        }
                    }
                }

                moduleData.put("totalCourses", totalCourses);
                moduleData.put("studentsCompleted", completedStudents.size());
                moduleData.put("completedBy", completedStudents);
                completionData.add(moduleData);
            }

            return ResponseEntity.ok(Map.of(
                "completions", completionData
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch completion data: " + e.getMessage()
            ));
        }
    }

    /**
     * PHASE 1: Get inactive students alert
     */
    @GetMapping("/inactive-students")
    public ResponseEntity<?> getInactiveStudents(
            @RequestParam(defaultValue = "7") int days,
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

            Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);
            List<UserEntity> students = userRepository.findByRole("STUDENT");
            List<Map<String, Object>> inactiveStudents = new ArrayList<>();

            for (UserEntity student : students) {
                List<CourseProgressEntity> allProgress = courseProgressRepository.findByUser(student);
                
                // Find last activity
                Optional<Instant> lastActivity = allProgress.stream()
                    .map(CourseProgressEntity::getStartedAt)
                    .filter(date -> date != null)
                    .max(Instant::compareTo);

                boolean isInactive = lastActivity.isEmpty() || lastActivity.get().isBefore(cutoffDate);

                if (isInactive) {
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("id", student.getId());
                    studentInfo.put("name", student.getName());
                    studentInfo.put("email", student.getEmail());
                    studentInfo.put("lastActivity", lastActivity.orElse(null));
                    studentInfo.put("daysSinceActivity", lastActivity.map(date -> 
                        ChronoUnit.DAYS.between(date, Instant.now())
                    ).orElse((long) -1));

                    // Get enrolled classes
                    List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                        .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(student);
                    List<String> classNames = enrollments.stream()
                        .map(e -> e.getClassEntity().getName())
                        .collect(Collectors.toList());
                    studentInfo.put("enrolledClasses", classNames);

                    // Progress stats
                    long completed = allProgress.stream().filter(CourseProgressEntity::isCompleted).count();
                    studentInfo.put("coursesStarted", allProgress.size());
                    studentInfo.put("coursesCompleted", completed);

                    inactiveStudents.add(studentInfo);
                }
            }

            // Sort by days since activity (most inactive first)
            inactiveStudents.sort((a, b) -> {
                Long daysA = (Long) a.get("daysSinceActivity");
                Long daysB = (Long) b.get("daysSinceActivity");
                return Long.compare(daysB, daysA);
            });

            return ResponseEntity.ok(Map.of(
                "inactiveStudents", inactiveStudents,
                "daysThreshold", days,
                "totalInactive", inactiveStudents.size()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch inactive students: " + e.getMessage()
            ));
        }
    }

    /**
     * PHASE 1: Get progress overview (students by completion stage) with filters
     */
    @GetMapping("/progress-overview")
    public ResponseEntity<?> getProgressOverview(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID trainingId,
            @RequestParam(required = false) UUID moduleId,
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

            List<UserEntity> students = userRepository.findByRole("STUDENT");
            
            // Get filter entities if provided
            ClassEntity classEntity = classId != null ? classRepository.findById(classId).orElse(null) : null;
            ModuleEntity training = trainingId != null ? moduleRepository.findById(trainingId).orElse(null) : null;
            TrainingModuleEntity module = moduleId != null ? trainingModuleRepository.findById(moduleId).orElse(null) : null;
            
            Map<String, List<Map<String, Object>>> progressBuckets = new HashMap<>();
            progressBuckets.put("notStarted", new ArrayList<>());
            progressBuckets.put("justStarted", new ArrayList<>());  // 1-24%
            progressBuckets.put("inProgress", new ArrayList<>());   // 25-74%
            progressBuckets.put("nearComplete", new ArrayList<>()); // 75-99%
            progressBuckets.put("completed", new ArrayList<>());    // 100%

            for (UserEntity student : students) {
                // Get progress based on filters
                List<CourseProgressEntity> progress = getFilteredProgress(student, classEntity, training, module);
                
                if (progress.isEmpty()) {
                    Map<String, Object> info = createStudentProgressInfo(student, 0, 0, 0);
                    info.put("classes", getStudentClasses(student));
                    progressBuckets.get("notStarted").add(info);
                    continue;
                }

                long total = progress.size();
                long completed = progress.stream().filter(CourseProgressEntity::isCompleted).count();
                double completionRate = total > 0 ? (double) completed / total * 100 : 0;

                Map<String, Object> info = createStudentProgressInfo(student, total, completed, completionRate);
                info.put("classes", getStudentClasses(student));

                if (completionRate == 0) {
                    progressBuckets.get("notStarted").add(info);
                } else if (completionRate < 25) {
                    progressBuckets.get("justStarted").add(info);
                } else if (completionRate < 75) {
                    progressBuckets.get("inProgress").add(info);
                } else if (completionRate < 100) {
                    progressBuckets.get("nearComplete").add(info);
                } else {
                    progressBuckets.get("completed").add(info);
                }
            }

            Map<String, Integer> counts = new HashMap<>();
            counts.put("notStarted", progressBuckets.get("notStarted").size());
            counts.put("justStarted", progressBuckets.get("justStarted").size());
            counts.put("inProgress", progressBuckets.get("inProgress").size());
            counts.put("nearComplete", progressBuckets.get("nearComplete").size());
            counts.put("completed", progressBuckets.get("completed").size());

            // Build filter info for response
            Map<String, Object> filterInfo = new HashMap<>();
            if (classEntity != null) {
                filterInfo.put("class", Map.of("id", classEntity.getId(), "name", classEntity.getName()));
            }
            if (training != null) {
                filterInfo.put("training", Map.of("id", training.getId(), "title", training.getTitle()));
            }
            if (module != null) {
                filterInfo.put("module", Map.of("id", module.getId(), "title", module.getTitle()));
            }

            return ResponseEntity.ok(Map.of(
                "progressBuckets", progressBuckets,
                "counts", counts,
                "totalStudents", students.size(),
                "filters", filterInfo
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch progress overview: " + e.getMessage()
            ));
        }
    }

    private List<CourseProgressEntity> getFilteredProgress(UserEntity student, ClassEntity classEntity, 
                                                           ModuleEntity training, TrainingModuleEntity module) {
        List<CourseProgressEntity> progress;

        if (classEntity != null) {
            // Filter by class
            progress = courseProgressRepository.findByUserAndClassEntity(student, classEntity);
            
            // Further filter by training if provided
            if (training != null) {
                progress = progress.stream()
                    .filter(p -> p.getCourse().getModule() != null && 
                                p.getCourse().getModule().getId().equals(training.getId()))
                    .collect(Collectors.toList());
            }
            
            // Further filter by module if provided
            if (module != null) {
                progress = progress.stream()
                    .filter(p -> p.getCourse().getTrainingModule() != null && 
                                p.getCourse().getTrainingModule().getId().equals(module.getId()))
                    .collect(Collectors.toList());
            }
        } else if (training != null) {
            // Filter by training only
            progress = courseProgressRepository.findByUser(student);
            progress = progress.stream()
                .filter(p -> p.getCourse().getModule() != null && 
                            p.getCourse().getModule().getId().equals(training.getId()))
                .collect(Collectors.toList());
                
            // Further filter by module if provided
            if (module != null) {
                progress = progress.stream()
                    .filter(p -> p.getCourse().getTrainingModule() != null && 
                                p.getCourse().getTrainingModule().getId().equals(module.getId()))
                    .collect(Collectors.toList());
            }
        } else if (module != null) {
            // Filter by module only
            progress = courseProgressRepository.findByUser(student);
            progress = progress.stream()
                .filter(p -> p.getCourse().getTrainingModule() != null && 
                            p.getCourse().getTrainingModule().getId().equals(module.getId()))
                .collect(Collectors.toList());
        } else {
            // No filters - get all progress
            progress = courseProgressRepository.findByUser(student);
        }

        return progress;
    }

    private List<String> getStudentClasses(UserEntity student) {
        List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
            .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(student);
        return enrollments.stream()
            .map(e -> e.getClassEntity().getName())
            .collect(Collectors.toList());
    }

    private Map<String, Object> createStudentProgressInfo(UserEntity student, long total, long completed, double rate) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", student.getId());
        info.put("name", student.getName());
        info.put("email", student.getEmail());
        info.put("imageUrl", student.getImageUrl());
        info.put("coursesTotal", total);
        info.put("coursesCompleted", completed);
        info.put("completionRate", Math.round(rate * 100) / 100.0);
        return info;
    }
    
    /**
     * Get available filters (classes, trainings, modules)
     */
    @GetMapping("/filters")
    public ResponseEntity<?> getAvailableFilters(Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }

            // Get all classes
            List<ClassEntity> classes = classRepository.findAll();
            List<Map<String, Object>> classList = classes.stream()
                .map(c -> Map.of(
                    "id", (Object) c.getId(),
                    "name", c.getName(),
                    "isActive", c.getIsActive()
                ))
                .collect(Collectors.toList());

            // Get all trainings (ModuleEntity)
            List<ModuleEntity> trainings = moduleRepository.findAll();
            List<Map<String, Object>> trainingList = trainings.stream()
                .map(t -> Map.of(
                    "id", (Object) t.getId(),
                    "title", t.getTitle()
                ))
                .collect(Collectors.toList());

            // Get all training modules
            List<TrainingModuleEntity> modules = trainingModuleRepository.findAll();
            List<Map<String, Object>> moduleList = modules.stream()
                .map(m -> Map.of(
                    "id", (Object) m.getId(),
                    "title", m.getTitle(),
                    "trainingId", m.getTraining().getId(),
                    "trainingName", m.getTraining().getTitle()
                ))
                .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "classes", classList,
                "trainings", trainingList,
                "modules", moduleList
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch filters: " + e.getMessage()
            ));
        }
    }

    /**
     * PHASE 1: Get assignment score analytics
     */
    @GetMapping("/scores")
    public ResponseEntity<?> getScoreAnalytics(Authentication auth) {
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
            List<Map<String, Object>> studentScores = new ArrayList<>();

            for (UserEntity student : students) {
                List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthor(student);
                List<PostEntity> scoredPosts = rateablePosts.stream()
                    .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                    .collect(Collectors.toList());

                if (!scoredPosts.isEmpty()) {
                    double avgScore = scoredPosts.stream()
                        .mapToDouble(p -> p.getAvgScore().doubleValue())
                        .average()
                        .orElse(0.0);

                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("id", student.getId());
                    studentInfo.put("name", student.getName());
                    studentInfo.put("email", student.getEmail());
                    studentInfo.put("totalAssignments", scoredPosts.size());
                    studentInfo.put("averageScore", Math.round(avgScore * 100) / 100.0);

                    // Score breakdown
                    long excellent = scoredPosts.stream().filter(p -> p.getAvgScore().doubleValue() >= 4.5).count();
                    long good = scoredPosts.stream().filter(p -> {
                        double score = p.getAvgScore().doubleValue();
                        return score >= 3.5 && score < 4.5;
                    }).count();
                    long average = scoredPosts.stream().filter(p -> {
                        double score = p.getAvgScore().doubleValue();
                        return score >= 2.5 && score < 3.5;
                    }).count();
                    long needsImprovement = scoredPosts.stream().filter(p -> p.getAvgScore().doubleValue() < 2.5).count();

                    studentInfo.put("excellent", excellent);      // 4.5-5.0
                    studentInfo.put("good", good);                // 3.5-4.4
                    studentInfo.put("average", average);          // 2.5-3.4
                    studentInfo.put("needsImprovement", needsImprovement); // < 2.5

                    studentScores.add(studentInfo);
                }
            }

            // Sort by average score (lowest first to identify students who need help)
            studentScores.sort((a, b) -> 
                Double.compare((Double) a.get("averageScore"), (Double) b.get("averageScore"))
            );

            // Calculate overall statistics
            double overallAvg = studentScores.stream()
                .mapToDouble(s -> (Double) s.get("averageScore"))
                .average()
                .orElse(0.0);

            return ResponseEntity.ok(Map.of(
                "studentScores", studentScores,
                "totalStudentsWithScores", studentScores.size(),
                "overallAverageScore", Math.round(overallAvg * 100) / 100.0
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch score analytics: " + e.getMessage()
            ));
        }
    }
}

