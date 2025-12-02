package com.thexperience.backend.codes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
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
    
    @PersistenceContext
    private EntityManager entityManager;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResponseEntity<?> generate(@PathVariable UUID classId, @RequestBody Map<String, Integer> body, Authentication auth) {
        System.out.println("🔑 [CODE-GEN] Generating code for class ID: " + classId);
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        
        // Deactivate all existing codes for this class
        List<ClassEnrollmentCodeEntity> existingCodes = codes.findByClassEntity(classEntity);
        System.out.println("📋 [CODE-GEN] Found " + existingCodes.size() + " existing codes to deactivate");
        if (!existingCodes.isEmpty()) {
            for (ClassEnrollmentCodeEntity code : existingCodes) {
                System.out.println("  ❌ Deactivating code ID: " + code.getId() + " (was active: " + code.isActive() + ")");
                code.setActive(false);
                codes.save(code);
            }
            codes.flush(); // Ensure deactivation is persisted before creating new code
            System.out.println("✓ [CODE-GEN] All existing codes deactivated and flushed");
        }
        
        // Generate ONE new active code (multi-use by default)
        String plain = codeUtils.generatePlainCode();
        String hash = codeUtils.sha256(plain);
        ClassEnrollmentCodeEntity e = new ClassEnrollmentCodeEntity();
        e.setClassEntity(classEntity);
        e.setCodeHash(hash);
        e.setPlainCode(plain); // Store plain code so admin can view it later
        e.setGeneratedBy(admin);
        e.setActive(true);
        e.setSingleUse(false); // CRITICAL FIX: Set to multi-use!
        System.out.println("💾 [CODE-GEN] Saving new code: " + plain + " (isActive: " + e.isActive() + ", isSingleUse: " + e.isSingleUse() + ")");
        ClassEnrollmentCodeEntity savedCode = codes.saveAndFlush(e); // Use saveAndFlush to ensure immediate persistence
        
        // Verify the code was saved correctly
        if (savedCode == null || savedCode.getId() == null) {
            System.err.println("❌ [CODE-GEN] CRITICAL ERROR: Failed to save enrollment code!");
            throw new RuntimeException("Failed to save enrollment code");
        }
        
        System.out.println("✅ [CODE-GEN] Code saved successfully! ID: " + savedCode.getId() + ", isActive: " + savedCode.isActive());
        return ResponseEntity.ok(Map.of("code", plain, "codeId", savedCode.getId()));
    }

    @GetMapping("/api/admin/classes/{classId}/codes")
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ResponseEntity<?> getActiveCode(@PathVariable UUID classId) {
        System.out.println("🔍 [CODE-GET] Getting active code for class ID: " + classId);
        
        // Clear entity manager cache to ensure we read fresh data
        entityManager.clear();
        
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        
        // Get the active code
        Optional<ClassEnrollmentCodeEntity> activeCodeOpt = codes.findFirstByClassEntityAndIsActiveTrue(classEntity);
        
        if (activeCodeOpt.isEmpty()) {
            System.out.println("⚠️ [CODE-GET] No active code found for class ID: " + classId);
            // Check if there are ANY codes (active or not)
            List<ClassEnrollmentCodeEntity> allCodes = codes.findByClassEntity(classEntity);
            System.out.println("📊 [CODE-GET] Total codes for this class: " + allCodes.size());
            for (ClassEnrollmentCodeEntity c : allCodes) {
                System.out.println("  - Code ID: " + c.getId() + ", isActive: " + c.isActive() + ", plainCode: " + (c.getPlainCode() != null ? "***" : "NULL"));
            }
            return ResponseEntity.ok(Map.of("hasCode", false));
        }
        
        ClassEnrollmentCodeEntity activeCode = activeCodeOpt.get();
        System.out.println("✅ [CODE-GET] Found active code! ID: " + activeCode.getId() + ", plainCode: " + (activeCode.getPlainCode() != null ? activeCode.getPlainCode() : "NULL"));
        
        // Get usage statistics
        long usageCount = usageLogRepository.countByCodeId(activeCode.getId());
        List<CodeUsageLogEntity> recentUsage = usageLogRepository.findByCodeIdOrderByUsedAtDesc(activeCode.getId());
        
        // Count all codes for this class
        List<ClassEnrollmentCodeEntity> allCodes = codes.findByClassEntity(classEntity);
        int totalCodes = allCodes.size();
        int activeCodes = (int) allCodes.stream().filter(ClassEnrollmentCodeEntity::isActive).count();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasCode", true);
        response.put("code", activeCode.getPlainCode()); // Return the plain code for admin to view
        response.put("codeId", activeCode.getId());
        response.put("createdAt", activeCode.getCreatedAt());
        response.put("generatedBy", activeCode.getGeneratedBy().getName());
        response.put("usageCount", usageCount);
        response.put("codesCount", totalCodes); // Total codes created
        response.put("unusedCount", activeCodes); // Active codes (unused)
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


