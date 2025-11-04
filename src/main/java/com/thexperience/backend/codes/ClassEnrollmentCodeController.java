package com.thexperience.backend.codes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ClassEnrollmentCodeController {
    private final ClassRepository classRepository;
    private final ClassEnrollmentCodeRepository codes;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository users;
    private final CodeUtils codeUtils;
    private final CodeUsageLogRepository usageLogRepository;

    public ClassEnrollmentCodeController(ClassRepository classRepository, ClassEnrollmentCodeRepository codes,
                                         ClassEnrollmentRepository enrollmentRepository, UserRepository users, 
                                         CodeUtils codeUtils, CodeUsageLogRepository usageLogRepository) {
        this.classRepository = classRepository;
        this.codes = codes;
        this.enrollmentRepository = enrollmentRepository;
        this.users = users;
        this.codeUtils = codeUtils;
        this.usageLogRepository = usageLogRepository;
    }

    @PostMapping("/api/admin/classes/{classId}/codes")
    public ResponseEntity<?> generate(@PathVariable UUID classId, Authentication auth) {
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        
        // Deactivate all existing codes for this class
        List<ClassEnrollmentCodeEntity> existingCodes = codes.findByClassEntity(classEntity);
        for (ClassEnrollmentCodeEntity code : existingCodes) {
            code.setActive(false);
            codes.save(code);
        }
        
        // Generate ONE new active code
        String plain = codeUtils.generatePlainCode();
        String hash = codeUtils.sha256(plain);
        ClassEnrollmentCodeEntity e = new ClassEnrollmentCodeEntity();
        e.setClassEntity(classEntity);
        e.setCodeHash(hash);
        e.setPlainCode(plain); // Store plain code so admin can view it later
        e.setGeneratedBy(admin);
        e.setActive(true);
        codes.save(e);
        
        return ResponseEntity.ok(Map.of("code", plain, "codeId", e.getId()));
    }

    @GetMapping("/api/admin/classes/{classId}/codes")
    public ResponseEntity<?> getActiveCode(@PathVariable UUID classId) {
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        
        // Get the active code
        Optional<ClassEnrollmentCodeEntity> activeCodeOpt = codes.findFirstByClassEntityAndIsActiveTrue(classEntity);
        
        if (activeCodeOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("hasCode", false));
        }
        
        ClassEnrollmentCodeEntity activeCode = activeCodeOpt.get();
        
        // Get usage statistics
        long usageCount = usageLogRepository.countByCodeId(activeCode.getId());
        List<CodeUsageLogEntity> recentUsage = usageLogRepository.findByCodeIdOrderByUsedAtDesc(activeCode.getId());
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasCode", true);
        response.put("code", activeCode.getPlainCode()); // Return the plain code for admin to view
        response.put("codeId", activeCode.getId());
        response.put("createdAt", activeCode.getCreatedAt());
        response.put("generatedBy", activeCode.getGeneratedBy().getName());
        response.put("usageCount", usageCount);
        response.put("recentUsage", recentUsage.stream().limit(10).map(usage -> {
            Map<String, Object> usageMap = new LinkedHashMap<>();
            usageMap.put("userName", usage.getUser().getName());
            usageMap.put("userEmail", usage.getUser().getEmail());
            usageMap.put("usedAt", usage.getUsedAt());
            return usageMap;
        }).toList());
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/admin/class-codes/{codeId}")
    public ResponseEntity<?> delete(@PathVariable UUID codeId) {
        ClassEnrollmentCodeEntity c = codes.findById(codeId).orElseThrow();
        if (c.getConsumedAt() != null) return ResponseEntity.badRequest().body(Map.of("error","Code already used"));
        codes.delete(c);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/classes/{classId}/enroll-with-code")
    public ResponseEntity<?> enrollWithCode(@PathVariable UUID classId, @RequestBody Map<String,String> body, Authentication auth) {
        String code = body.getOrDefault("code", "");
        if (code.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Code required"));
        
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        String hash = codeUtils.sha256(code);
        
        // Check if the code is valid and active
        ClassEnrollmentCodeEntity c = codes.findFirstByClassEntityAndCodeHashAndIsActiveTrue(classEntity, hash).orElse(null);
        
        if (c == null) return ResponseEntity.status(400).body(Map.of("error","Invalid or inactive code"));
        
        UserEntity user = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        
        // Check if user is already enrolled in this class
        boolean alreadyEnrolled = enrollmentRepository.findByStudentAndClassEntity(user, classEntity).isPresent();
        if (alreadyEnrolled) {
            return ResponseEntity.ok(Map.of("enrolled", true, "message", "You are already enrolled in this class"));
        }
        
        // Log code usage (don't mark as consumed - allow reuse!)
        CodeUsageLogEntity usageLog = new CodeUsageLogEntity();
        usageLog.setCodeId(c.getId());
        usageLog.setUser(user);
        usageLog.setCodeType("CLASS_ENROLLMENT");
        usageLog.setResourceId(classId);
        usageLogRepository.save(usageLog);
        
        // Create enrollment
        ClassEnrollmentEntity enrollment = new ClassEnrollmentEntity(user, classEntity);
        enrollmentRepository.save(enrollment);
        
        return ResponseEntity.ok(Map.of("enrolled", true, "message", "Successfully enrolled in class"));
    }
}


