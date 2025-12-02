package com.thexperience.backend.leaderboard;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.PostRepository;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.repos.ClassRepository;
import com.thexperience.backend.repos.ClassEnrollmentRepository;
import com.thexperience.backend.repos.PostLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug/leaderboard")
public class LeaderboardDiagnostics {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    /**
     * Comprehensive diagnostic endpoint to check leaderboard data
     */
    @GetMapping("/diagnose")
    public ResponseEntity<?> diagnoseLeaderboardIssues(Authentication auth) {
        Map<String, Object> diagnostics = new HashMap<>();

        try {
            // 1. Check all posts
            List<PostEntity> allPosts = postRepository.findAll();
            diagnostics.put("totalPosts", allPosts.size());

            // 2. Check rateable posts (by postType - ASSIGNMENT or WEEKLY_SUMMARY)
            long assignmentsByType = allPosts.stream()
                .filter(p -> p.getPostType() == PostEntity.PostType.ASSIGNMENT)
                .count();
            diagnostics.put("assignmentsByPostType", assignmentsByType);
            
            long weeklySummariesByType = allPosts.stream()
                .filter(p -> p.getPostType() == PostEntity.PostType.WEEKLY_SUMMARY)
                .count();
            diagnostics.put("weeklySummariesByPostType", weeklySummariesByType);
            
            long rateablePostsByType = assignmentsByType + weeklySummariesByType;
            diagnostics.put("rateablePostsTotal", rateablePostsByType);

            // 3. Check assignment posts (by isAssignment flag)
            long assignmentsByFlag = allPosts.stream()
                .filter(p -> p.getIsAssignment() != null && p.getIsAssignment())
                .count();
            diagnostics.put("assignmentsByIsAssignmentFlag", assignmentsByFlag);

            // 4. Check mismatches
            List<Map<String, Object>> mismatches = allPosts.stream()
                .filter(p -> {
                    boolean hasAssignmentType = p.getPostType() == PostEntity.PostType.ASSIGNMENT;
                    boolean hasAssignmentFlag = p.getIsAssignment() != null && p.getIsAssignment();
                    return hasAssignmentType != hasAssignmentFlag;
                })
                .map(p -> {
                    Map<String, Object> mismatch = new HashMap<>();
                    mismatch.put("id", p.getId().toString());
                    mismatch.put("title", p.getTitle());
                    mismatch.put("postType", p.getPostType().toString());
                    mismatch.put("isAssignment", p.getIsAssignment());
                    mismatch.put("author", p.getAuthor().getName());
                    mismatch.put("classId", p.getClassEntity() != null ? p.getClassEntity().getId().toString() : "null");
                    mismatch.put("className", p.getClassEntity() != null ? p.getClassEntity().getName() : "null");
                    return mismatch;
                })
                .collect(Collectors.toList());
            diagnostics.put("postTypeFlagMismatches", mismatches);
            diagnostics.put("mismatchCount", mismatches.size());

            // 5. Check rated rateable posts (ASSIGNMENT + WEEKLY_SUMMARY)
            List<PostEntity> ratedRateablePosts = allPosts.stream()
                .filter(p -> p.getPostType() == PostEntity.PostType.ASSIGNMENT || p.getPostType() == PostEntity.PostType.WEEKLY_SUMMARY)
                .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                .collect(Collectors.toList());
            diagnostics.put("ratedRateablePostsCount", ratedRateablePosts.size());

            // 6. Check unrated rateable posts
            List<Map<String, Object>> unratedAssignments = allPosts.stream()
                .filter(p -> p.getPostType() == PostEntity.PostType.ASSIGNMENT || p.getPostType() == PostEntity.PostType.WEEKLY_SUMMARY)
                .filter(p -> p.getAvgScore() == null || p.getScoreCount() == null || p.getScoreCount() == 0)
                .map(p -> {
                    Map<String, Object> unrated = new HashMap<>();
                    unrated.put("id", p.getId().toString());
                    unrated.put("title", p.getTitle());
                    unrated.put("author", p.getAuthor().getName());
                    unrated.put("postType", p.getPostType().toString());
                    unrated.put("avgScore", p.getAvgScore());
                    unrated.put("scoreCount", p.getScoreCount());
                    unrated.put("classId", p.getClassEntity() != null ? p.getClassEntity().getId().toString() : "null");
                    unrated.put("className", p.getClassEntity() != null ? p.getClassEntity().getName() : "null");
                    
                    // Count how many ratings this post actually has
                    List<PostLikeEntity> ratings = postLikeRepository.findByPost(p);
                    long ratingsWithScores = ratings.stream()
                        .filter(r -> r.getRating() != null)
                        .count();
                    unrated.put("actualRatingsCount", ratingsWithScores);
                    
                    return unrated;
                })
                .collect(Collectors.toList());
            diagnostics.put("unratedAssignments", unratedAssignments);

            // 7. Check assignments without class
            long assignmentsWithoutClass = allPosts.stream()
                .filter(p -> p.getIsAssignment() != null && p.getIsAssignment())
                .filter(p -> p.getClassEntity() == null)
                .count();
            diagnostics.put("assignmentsWithoutClass", assignmentsWithoutClass);

            // 8. Check classes
            List<ClassEntity> allClasses = classRepository.findAll();
            diagnostics.put("totalClasses", allClasses.size());

            // 9. Check class enrollments
            List<ClassEnrollmentEntity> allEnrollments = classEnrollmentRepository.findAll();
            diagnostics.put("totalEnrollments", allEnrollments.size());

            // 10. For each class, check if there are rated assignments
            List<Map<String, Object>> classStats = allClasses.stream()
                .map(classEntity -> {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("id", classEntity.getId().toString());
                    stats.put("name", classEntity.getName());
                    
                    long enrollmentCount = classEnrollmentRepository
                        .findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classEntity).size();
                    stats.put("enrollmentCount", enrollmentCount);
                    
                    List<PostEntity> classPosts = postRepository.findByClassEntityIdAndIsHiddenFalseOrderByIsPinnedDescCreatedAtDesc(
                        classEntity.getId(), org.springframework.data.domain.PageRequest.of(0, 1000)
                    ).getContent();
                    stats.put("totalPosts", classPosts.size());
                    
                    long classAssignments = classPosts.stream()
                        .filter(p -> p.getIsAssignment() != null && p.getIsAssignment())
                        .count();
                    stats.put("assignmentCount", classAssignments);
                    
                    long ratedClassAssignments = classPosts.stream()
                        .filter(p -> p.getIsAssignment() != null && p.getIsAssignment())
                        .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                        .count();
                    stats.put("ratedAssignmentCount", ratedClassAssignments);
                    
                    return stats;
                })
                .collect(Collectors.toList());
            diagnostics.put("classStats", classStats);

            // 11. Check students with rated assignments
            List<UserEntity> students = userRepository.findAll().stream()
                .filter(u -> "STUDENT".equals(u.getRole()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> studentStats = students.stream()
                .map(student -> {
                    List<PostEntity> studentRateablePosts = postRepository.findRateablePostsByAuthor(student);
                    long ratedCount = studentRateablePosts.stream()
                        .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                        .count();
                    
                    if (studentRateablePosts.size() > 0) {
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("name", student.getName());
                        stats.put("totalRateablePosts", studentRateablePosts.size());
                        stats.put("ratedPosts", ratedCount);
                        
                        double totalPoints = studentRateablePosts.stream()
                            .filter(p -> p.getAvgScore() != null && p.getScoreCount() != null && p.getScoreCount() > 0)
                            .mapToDouble(p -> p.getAvgScore().doubleValue())
                            .sum();
                        stats.put("totalPoints", totalPoints);
                        
                        return stats;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            diagnostics.put("studentsWithAssignments", studentStats);

            return ResponseEntity.ok(diagnostics);
        } catch (Exception e) {
            diagnostics.put("error", e.getMessage());
            diagnostics.put("stackTrace", Arrays.toString(e.getStackTrace()));
            return ResponseEntity.internalServerError().body(diagnostics);
        }
    }

    /**
     * Fix endpoint to sync isAssignment flag with postType
     */
    @PostMapping("/fix-assignment-flags")
    public ResponseEntity<?> fixAssignmentFlags(Authentication auth) {
        try {
            List<PostEntity> allPosts = postRepository.findAll();
            int fixedCount = 0;

            for (PostEntity post : allPosts) {
                boolean shouldBeAssignment = post.getPostType() == PostEntity.PostType.ASSIGNMENT;
                boolean isMarkedAsAssignment = post.getIsAssignment() != null && post.getIsAssignment();

                if (shouldBeAssignment && !isMarkedAsAssignment) {
                    post.setIsAssignment(true);
                    postRepository.save(post);
                    fixedCount++;
                    System.out.println("Fixed post " + post.getId() + ": set isAssignment=true");
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("fixedCount", fixedCount);
            result.put("message", "Fixed " + fixedCount + " posts");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Recalculate all assignment scores
     */
    @PostMapping("/recalculate-scores")
    public ResponseEntity<?> recalculateScores(Authentication auth) {
        try {
            List<PostEntity> assignments = postRepository.findAll().stream()
                .filter(p -> p.getIsAssignment() != null && p.getIsAssignment())
                .collect(Collectors.toList());

            int updatedCount = 0;

            for (PostEntity post : assignments) {
                List<PostLikeEntity> allRatings = postLikeRepository.findByPost(post);
                List<PostLikeEntity> ratingsWithScores = allRatings.stream()
                    .filter(r -> r.getRating() != null)
                    .collect(Collectors.toList());

                if (!ratingsWithScores.isEmpty()) {
                    double average = ratingsWithScores.stream()
                        .mapToInt(PostLikeEntity::getRating)
                        .average()
                        .orElse(0.0);

                    post.setAvgScore(java.math.BigDecimal.valueOf(average).setScale(2, java.math.RoundingMode.HALF_UP));
                    post.setScoreCount(ratingsWithScores.size());
                    postRepository.save(post);
                    updatedCount++;
                    System.out.println("Updated post " + post.getId() + ": avgScore=" + post.getAvgScore() + ", scoreCount=" + post.getScoreCount());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("updatedCount", updatedCount);
            result.put("message", "Recalculated scores for " + updatedCount + " assignments");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

