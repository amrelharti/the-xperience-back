package com.thexperience.backend.codes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import com.thexperience.backend.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/courses")
public class CourseAccessCodeController {
    
    private final CourseAccessCodeRepository courseAccessCodeRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ModuleProgressRepository moduleProgressRepository;
    private final JwtService jwtService;
    
    public CourseAccessCodeController(
            CourseAccessCodeRepository courseAccessCodeRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            ModuleProgressRepository moduleProgressRepository,
            JwtService jwtService) {
        this.courseAccessCodeRepository = courseAccessCodeRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.moduleProgressRepository = moduleProgressRepository;
        this.jwtService = jwtService;
    }
    
    // Generate codes for a course
    @PostMapping("/{courseId}/access-codes")
    public ResponseEntity<?> generateCodes(@PathVariable java.util.UUID courseId, 
                                         @RequestBody GenerateCodesRequest request,
                                         Authentication auth) {
        try {
            // Get user ID from authentication
            String userId = auth.getName();
            UserEntity admin = userRepository.findById(java.util.UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));
            
            // Find the course
            CourseEntity course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Generate codes
            List<String> codes = new ArrayList<>();
            for (int i = 0; i < request.count(); i++) {
                String code = generateRandomCode();
                String codeHash = hashCode(code);
                
                CourseAccessCodeEntity codeEntity = new CourseAccessCodeEntity(course, codeHash, admin);
                courseAccessCodeRepository.save(codeEntity);
                codes.add(code);
            }
            
            return ResponseEntity.ok(Map.of("codes", codes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating codes: " + e.getMessage());
        }
    }
    
    // List codes for a course
    @GetMapping("/{courseId}/access-codes")
    public ResponseEntity<?> listCodes(@PathVariable java.util.UUID courseId, Authentication auth) {
        try {
            CourseEntity course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));
            
            List<CourseAccessCodeEntity> codes = courseAccessCodeRepository.findByCourseOrderByCreatedAtDesc(course);
            
            List<Map<String, Object>> codeList = codes.stream().map(code -> {
                Map<String, Object> codeMap = new HashMap<>();
                codeMap.put("id", code.getId());
                codeMap.put("createdAt", code.getCreatedAt());
                codeMap.put("consumedAt", code.getConsumedAt());
                codeMap.put("isConsumed", code.getConsumedAt() != null);
                codeMap.put("generatedBy", code.getGeneratedBy().getName());
                codeMap.put("consumedBy", code.getConsumedBy() != null ? code.getConsumedBy().getName() : null);
                return codeMap;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(codeList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error listing codes: " + e.getMessage());
        }
    }
    
    // Delete a code
    @DeleteMapping("/access-codes/{codeId}")
    public ResponseEntity<?> deleteCode(@PathVariable java.util.UUID codeId, Authentication auth) {
        try {
            courseAccessCodeRepository.deleteById(codeId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting code: " + e.getMessage());
        }
    }
    
    
    private String generateRandomCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
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
    
    public record GenerateCodesRequest(int count) {}
}
