package com.thexperience.backend.leaderboard;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.repos.HomeworkRepository;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.repos.PostRepository;
import com.thexperience.backend.repos.PostLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;
    
    @Autowired
    private com.thexperience.backend.repos.ClassRepository classRepository;
    
    @Autowired
    private com.thexperience.backend.repos.ClassEnrollmentRepository classEnrollmentRepository;

    // Get global leaderboard
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getGlobalLeaderboard(
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        try {
            System.out.println("=== LEADERBOARD DEBUG ===");
            System.out.println("Calculating global leaderboard with limit: " + limit);
            
            List<Map<String, Object>> leaderboard = calculateLeaderboard(null, limit);
            
            System.out.println("Leaderboard size: " + leaderboard.size());
            for (Map<String, Object> entry : leaderboard) {
                System.out.println("Entry: " + entry);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("leaderboard", leaderboard);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in leaderboard: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch leaderboard: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get class-specific leaderboard (UPDATED: class-based, not module)
    @GetMapping("/leaderboard/class/{classId}")
    public ResponseEntity<?> getClassLeaderboard(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) UUID training,
            Authentication auth) {
        try {
            System.out.println("=== CLASS LEADERBOARD REQUEST ===");
            System.out.println("Class ID: " + classId);
            
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            System.out.println("Class found: " + classEntity.getName());
            
            if (training != null) {
                System.out.println("Training filter: " + training);
            }
            
            List<Map<String, Object>> leaderboard = calculateClassLeaderboard(classEntity, limit, training);
            
            Map<String, Object> classMap = new HashMap<>();
            classMap.put("id", classEntity.getId().toString());
            classMap.put("name", classEntity.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("leaderboard", leaderboard);
            response.put("class", classMap);
            
            System.out.println("Returning " + leaderboard.size() + " students in leaderboard");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in class leaderboard: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch class leaderboard: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    // DEPRECATED: Redirects to global leaderboard (module-based leaderboard removed in favor of class-based)
    @GetMapping("/leaderboard/module/{moduleId}")
    public ResponseEntity<?> getModuleLeaderboard(
            @PathVariable UUID moduleId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication auth) {
        // Module-based leaderboard is deprecated in favor of class-based leaderboard
        // Return global leaderboard for backward compatibility
        return getGlobalLeaderboard(limit, auth);
    }

    // Get user's rank in global leaderboard
    @GetMapping("/leaderboard/my-rank")
    public ResponseEntity<?> getMyGlobalRank(Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            List<Map<String, Object>> leaderboard = calculateLeaderboard(null, Integer.MAX_VALUE);
            int rank = findUserRank(leaderboard, userId);
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId().toString());
            userMap.put("name", user.getName());
            userMap.put("totalPoints", calculateUserTotalPoints(user));
            
            Map<String, Object> response = new HashMap<>();
            response.put("rank", rank);
            response.put("user", userMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get rank: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get user's rank in module leaderboard
    @GetMapping("/leaderboard/module/{moduleId}/my-rank")
    public ResponseEntity<?> getMyModuleRank(@PathVariable UUID moduleId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            ModuleEntity module = moduleRepository.findById(moduleId).orElseThrow();
            
            List<Map<String, Object>> leaderboard = calculateLeaderboard(module, Integer.MAX_VALUE);
            int rank = findUserRank(leaderboard, userId);
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId().toString());
            userMap.put("name", user.getName());
            userMap.put("totalPoints", calculateUserModulePoints(user, module));
            
            Map<String, Object> moduleMap = new HashMap<>();
            moduleMap.put("id", module.getId().toString());
            moduleMap.put("title", module.getTitle());
            
            Map<String, Object> response = new HashMap<>();
            response.put("rank", rank);
            response.put("user", userMap);
            response.put("module", moduleMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get module rank: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Calculate leaderboard for global or module-specific
    private List<Map<String, Object>> calculateLeaderboard(ModuleEntity module, int limit) {
        List<UserEntity> users = userRepository.findAll();
        System.out.println("Total users found: " + users.size());
        
        List<Map<String, Object>> leaderboard = users.stream()
            .filter(user -> "STUDENT".equals(user.getRole()))
            .map(user -> {
                double totalPoints = module != null ? 
                    calculateUserModulePoints(user, module) : 
                    calculateUserTotalPoints(user);
                
                int assignmentCount = module != null ? 
                    getAssignmentCountForUserInModule(user, module) :
                    getAssignmentCountForUser(user);
                
                // Debug: show total rateable posts vs rated posts
                List<PostEntity> allRateablePosts = module != null ? 
                    postRepository.findRateablePostsByAuthorAndModule(user, module) :
                    postRepository.findRateablePostsByAuthor(user);
                long ratedPosts = allRateablePosts.stream()
                    .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
                    .count();
                
                System.out.println("User: " + user.getName() + 
                    ", Points: " + totalPoints + 
                    ", Rated Posts (ASSIGNMENT + WEEKLY_SUMMARY): " + assignmentCount + 
                    ", Total Rateable Posts: " + allRateablePosts.size() +
                    ", Unrated: " + (allRateablePosts.size() - ratedPosts));
                
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId().toString());
                userMap.put("name", user.getName());
                userMap.put("imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "");
                
                Map<String, Object> entry = new HashMap<>();
                entry.put("user", userMap);
                entry.put("totalPoints", totalPoints);
                entry.put("assignmentCount", assignmentCount);
                entry.put("avgScore", assignmentCount > 0 ? totalPoints / assignmentCount : 0.0);
                return entry;
            })
            .filter(entry -> (Double) entry.get("totalPoints") > 0)
            .sorted((a, b) -> Double.compare((Double) b.get("totalPoints"), (Double) a.get("totalPoints")))
            .limit(limit)
            .collect(Collectors.toList());

        System.out.println("Filtered leaderboard size: " + leaderboard.size());

        // Add rank numbers
        for (int i = 0; i < leaderboard.size(); i++) {
            Map<String, Object> entry = new HashMap<>(leaderboard.get(i));
            entry.put("rank", i + 1);
            leaderboard.set(i, entry);
        }

        return leaderboard;
    }
    
    // Calculate leaderboard for a specific class (NEW: class-based rankings)
    private List<Map<String, Object>> calculateClassLeaderboard(ClassEntity classEntity, int limit, UUID trainingId) {
        System.out.println("=== CALCULATING CLASS LEADERBOARD ===");
        System.out.println("Class: " + classEntity.getName() + " (ID: " + classEntity.getId() + ")");
        
        // Get all students enrolled in this class
        List<com.thexperience.backend.entities.ClassEnrollmentEntity> enrollments = 
            classEnrollmentRepository.findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classEntity);
        
        System.out.println("Total enrollments in class: " + enrollments.size());
        
        List<Map<String, Object>> leaderboard = enrollments.stream()
            .map(enrollment -> enrollment.getStudent())
            .filter(user -> "STUDENT".equals(user.getRole()))
            .map(user -> {
                // Calculate points from rateable posts (ASSIGNMENT + WEEKLY_SUMMARY) in THIS class only
                List<PostEntity> classPosts;
                // Use class-based filtering for rateable posts
                classPosts = postRepository.findRateablePostsByAuthorAndClass(user, classEntity);
                System.out.println("User " + user.getName() + " has " + classPosts.size() + " rateable posts in class " + classEntity.getName());
                
                if (trainingId != null) {
                    System.out.println("Note: Training filtering (" + trainingId + ") not yet implemented due to data structure issues");
                }
                
                double totalPoints = classPosts.stream()
                    .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
                    .mapToDouble(post -> post.getAvgScore().doubleValue())
                    .sum();
                
                int assignmentCount = (int) classPosts.stream()
                    .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
                    .count();
                
                System.out.println("User: " + user.getName() + 
                    ", Class Posts: " + classPosts.size() +
                    ", Rated: " + assignmentCount + 
                    ", Points: " + totalPoints);
                
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId().toString());
                userMap.put("name", user.getName());
                userMap.put("imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "");
                
                Map<String, Object> entry = new HashMap<>();
                entry.put("user", userMap);
                entry.put("totalPoints", totalPoints);
                entry.put("assignmentCount", assignmentCount);
                entry.put("avgScore", assignmentCount > 0 ? totalPoints / assignmentCount : 0.0);
                return entry;
            })
            .filter(entry -> (Double) entry.get("totalPoints") > 0) // Only include students with points
            .sorted((a, b) -> Double.compare((Double) b.get("totalPoints"), (Double) a.get("totalPoints"))) // Sort descending
            .limit(limit)
            .collect(Collectors.toList());
        
        System.out.println("Leaderboard size after filtering: " + leaderboard.size());
        
        // Add rank numbers
        for (int i = 0; i < leaderboard.size(); i++) {
            Map<String, Object> entry = new HashMap<>(leaderboard.get(i));
            entry.put("rank", i + 1);
            leaderboard.set(i, entry);
        }
        
        return leaderboard;
    }

    // Calculate total points for a user based on assignment and weekly summary ratings they received
    private double calculateUserTotalPoints(UserEntity user) {
        // First, let's check ALL posts by this user
        List<PostEntity> allPosts = postRepository.findByAuthor(user);
        System.out.println("=== USER POINTS DEBUG ===");
        System.out.println("User: " + user.getName() + " (ID: " + user.getId() + ")");
        System.out.println("Total posts found: " + allPosts.size());
        
        for (PostEntity post : allPosts) {
            System.out.println("Post ID: " + post.getId() + 
                ", Title: " + post.getTitle() + 
                ", AvgScore: " + post.getAvgScore() + 
                ", ScoreCount: " + post.getScoreCount() +
                ", PostType: " + post.getPostType());
        }
        
        // Now check rateable posts (ASSIGNMENT or WEEKLY_SUMMARY)
        List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthor(user);
        System.out.println("Total rateable posts (ASSIGNMENT + WEEKLY_SUMMARY) found: " + rateablePosts.size());
        
        double totalPoints = rateablePosts.stream()
            .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
            .mapToDouble(post -> {
                System.out.println("Including post " + post.getId() + " (" + post.getPostType() + ") with score: " + post.getAvgScore());
                return post.getAvgScore().doubleValue();
            })
            .sum();
            
        System.out.println("Total points for " + user.getName() + ": " + totalPoints);
        return totalPoints;
    }

    // Calculate total points for a user in a specific module based on assignment and weekly summary ratings
    private double calculateUserModulePoints(UserEntity user, ModuleEntity module) {
        if (module == null) return 0.0;
        
        List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthorAndModule(user, module);
        return rateablePosts.stream()
            .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
            .mapToDouble(post -> post.getAvgScore().doubleValue())
            .sum();
    }

    // Get rateable post count for a user across all modules (only rated assignments and weekly summaries)
    private int getAssignmentCountForUser(UserEntity user) {
        List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthor(user);
        return (int) rateablePosts.stream()
            .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
            .count();
    }

    // Get rateable post count for a user in a specific module (only rated assignments and weekly summaries)
    private int getAssignmentCountForUserInModule(UserEntity user, ModuleEntity module) {
        if (module == null) return 0;
        
        List<PostEntity> rateablePosts = postRepository.findRateablePostsByAuthorAndModule(user, module);
        return (int) rateablePosts.stream()
            .filter(post -> post.getAvgScore() != null && post.getScoreCount() != null && post.getScoreCount() > 0)
            .count();
    }

    // Find user's rank in leaderboard
    private int findUserRank(List<Map<String, Object>> leaderboard, UUID userId) {
        for (int i = 0; i < leaderboard.size(); i++) {
            Map<String, Object> entry = leaderboard.get(i);
            Object userObj = entry.get("user");
            if (userObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) userObj;
                Object idObj = user.get("id");
                if (idObj != null && userId.toString().equals(idObj.toString())) {
                    return i + 1;
                }
            }
        }
        return -1; // User not found in leaderboard
    }
    
    // Temporary fix endpoint to mark a post as assignment
    @PostMapping("/fix-assignment/{postId}")
    public ResponseEntity<?> fixAssignment(@PathVariable UUID postId) {
        try {
            PostEntity post = postRepository.findById(postId).orElse(null);
            if (post == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Post not found"));
            }
            
            System.out.println("Fixing post: " + post.getId() + ", Title: " + post.getTitle() + 
                ", Current isAssignment: " + post.getIsAssignment());
            
            post.setIsAssignment(true);
            postRepository.save(post);
            
            System.out.println("Post fixed! New isAssignment: " + post.getIsAssignment());
            
            return ResponseEntity.ok(Map.of("success", true, "postId", postId, "isAssignment", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
