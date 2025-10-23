package com.thexperience.backend.homework;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import com.thexperience.backend.homework.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private HomeworkScoreRepository homeworkScoreRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // Create homework
    @PostMapping
    public ResponseEntity<?> createHomework(@RequestBody CreateHomeworkRequest request, Authentication auth) {
        try {
            System.out.println("Creating homework with request: " + request);
            System.out.println("ModuleId: " + request.getModuleId());
            System.out.println("Title: " + request.getTitle());
            System.out.println("Body: " + request.getBody());
            System.out.println("Attachments: " + request.getAttachments());
            
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            ModuleEntity module = moduleRepository.findById(request.getModuleId()).orElseThrow();

            HomeworkEntity homework = new HomeworkEntity(user, module, request.getTitle(), request.getBody());
            homework.setAttachments(request.getAttachments() != null ? request.getAttachments() : "");
            
            homework = homeworkRepository.save(homework);
            
            return ResponseEntity.ok(Map.of(
                "id", homework.getId(),
                "title", homework.getTitle(),
                "body", homework.getBody(),
                "author", Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "imageUrl", user.getImageUrl()
                ),
                "module", Map.of(
                    "id", module.getId(),
                    "title", module.getTitle()
                ),
                "createdAt", homework.getCreatedAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create homework: " + e.getMessage()));
        }
    }

    // Get homework by module
    @GetMapping
    public ResponseEntity<?> getHomeworks(
            @RequestParam(required = false) UUID moduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HomeworkEntity> homeworks;

            if (moduleId != null) {
                ModuleEntity module = moduleRepository.findById(moduleId).orElseThrow();
                homeworks = homeworkRepository.findByModuleAndIsHiddenFalseOrderByCreatedAtDesc(module, pageable);
            } else {
                homeworks = homeworkRepository.findByIsHiddenFalseOrderByCreatedAtDesc(pageable);
            }

            List<Map<String, Object>> homeworkList = homeworks.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("homeworks", homeworkList);
            response.put("totalPages", homeworks.getTotalPages());
            response.put("totalElements", homeworks.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch homeworks: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get global feed (moved to FeedController)
    // This endpoint has been moved to /api/feed in FeedController

    // Score homework
    @PostMapping("/{homeworkId}/score")
    @Transactional
    public ResponseEntity<?> scoreHomework(
            @PathVariable UUID homeworkId,
            @RequestBody ScoreHomeworkRequest request,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            HomeworkEntity homework = homeworkRepository.findById(homeworkId).orElseThrow();

            // Prevent self-scoring
            if (homework.getAuthor().getId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cannot score your own homework");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate score range
            if (request.getValue() < 0 || request.getValue() > 10) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Score must be between 0 and 10");
                return ResponseEntity.badRequest().body(error);
            }

            // Find existing score or create new one
            Optional<HomeworkScoreEntity> existingScore = homeworkScoreRepository.findByHomeworkAndUser(homework, user);
            HomeworkScoreEntity score;
            
            if (existingScore.isPresent()) {
                score = existingScore.get();
                score.setValue(request.getValue());
            } else {
                score = new HomeworkScoreEntity(homework, user, request.getValue());
            }
            
            homeworkScoreRepository.save(score);

            // Recalculate average score
            Double avgScore = homeworkScoreRepository.calculateAverageScore(homework);
            Long scoreCount = homeworkScoreRepository.countByHomework(homework);
            
            // Update score fields directly on the entity
            BigDecimal newAvgScore = BigDecimal.valueOf(avgScore != null ? avgScore : 0.0).setScale(2, RoundingMode.HALF_UP);
            Integer newScoreCount = scoreCount.intValue();
            
            System.out.println("Updating score fields for homework: " + homework.getId());
            System.out.println("New avg score: " + newAvgScore);
            System.out.println("New score count: " + newScoreCount);
            
            // Update the entity and save
            homework.setAvgScore(newAvgScore);
            homework.setScoreCount(newScoreCount);
            homeworkRepository.save(homework);

            Map<String, Object> response = new HashMap<>();
            response.put("homeworkId", homeworkId.toString());
            response.put("score", request.getValue());
            response.put("avgScore", homework.getAvgScore());
            response.put("scoreCount", homework.getScoreCount());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to score homework: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get user's score for a homework
    @GetMapping("/{homeworkId}/my-score")
    public ResponseEntity<?> getMyScore(@PathVariable UUID homeworkId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            HomeworkEntity homework = homeworkRepository.findById(homeworkId).orElseThrow();

            Optional<HomeworkScoreEntity> score = homeworkScoreRepository.findByHomeworkAndUser(homework, user);
            
            if (score.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("score", score.get().getValue());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("score", null);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get score: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Helper method to convert entity to response
    private Map<String, Object> convertToResponse(HomeworkEntity homework) {
        Map<String, Object> author = new HashMap<>();
        author.put("id", homework.getAuthor().getId().toString());
        author.put("name", homework.getAuthor().getName());
        author.put("imageUrl", homework.getAuthor().getImageUrl() != null ? homework.getAuthor().getImageUrl() : "");
        
        Map<String, Object> module = new HashMap<>();
        module.put("id", homework.getModule().getId().toString());
        module.put("title", homework.getModule().getTitle());
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", homework.getId().toString());
        response.put("title", homework.getTitle());
        response.put("body", homework.getBody());
        response.put("attachments", homework.getAttachments() != null ? homework.getAttachments() : "");
        response.put("avgScore", homework.getAvgScore());
        response.put("scoreCount", homework.getScoreCount());
        response.put("isHidden", homework.getIsHidden());
        response.put("createdAt", homework.getCreatedAt().toString());
        response.put("author", author);
        response.put("module", module);
        
        return response;
    }

}
