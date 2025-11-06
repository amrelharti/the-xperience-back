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

/**
 * Controller for managing multi-use, class-specific training module access codes.
 * Each code is tied to a TrainingModule + Class combination.
 */
@RestController
@RequestMapping("/api/admin/training-modules")
public class TrainingModuleAccessCodeController {
    private final TrainingModuleRepository trainingModules;
    private final ModuleAccessCodeRepository codes;
    private final UserRepository users;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ModuleProgressRepository progressRepo;
    private final CodeUtils codeUtils;
    private final CodeUsageLogRepository usageLogRepository;

    public TrainingModuleAccessCodeController(
            TrainingModuleRepository trainingModules,
            ModuleAccessCodeRepository codes,
            UserRepository users,
            ClassRepository classRepository,
            ClassEnrollmentRepository enrollmentRepository,
            ModuleProgressRepository progressRepo,
            CodeUtils codeUtils,
            CodeUsageLogRepository usageLogRepository) {
        this.trainingModules = trainingModules;
        this.codes = codes;
        this.users = users;
        this.classRepository = classRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepo = progressRepo;
        this.codeUtils = codeUtils;
        this.usageLogRepository = usageLogRepository;
    }

    /**
     * Generate a new access code for a specific training module and class.
     * Deactivates any existing codes for this combination.
     */
    @PostMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> generateCode(
            @PathVariable UUID trainingModuleId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            // Get classId from request body
            String classIdStr = (String) body.get("classId");
            if (classIdStr == null || classIdStr.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            
            UUID classId = UUID.fromString(classIdStr);
            
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));
            
            // Verify that the class belongs to the same training
            if (!classEntity.getModule().getId().equals(trainingModule.getTraining().getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Class does not belong to this training"));
            }
            
            UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName())))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Deactivate existing code for this training module + class combination
            List<ModuleAccessCodeEntity> existingCodes = codes.findByTrainingModuleAndClassEntity(
                trainingModule, classEntity
            );
            for (ModuleAccessCodeEntity code : existingCodes) {
                code.setActive(false);
                codes.save(code);
            }
            
            // Generate ONE new active code (multi-use)
            String plain = codeUtils.generatePlainCode();
            String hash = codeUtils.sha256(plain);
            
            ModuleAccessCodeEntity codeEntity = new ModuleAccessCodeEntity();
            codeEntity.setModule(trainingModule.getTraining()); // Parent training
            codeEntity.setTrainingModule(trainingModule); // Specific training module
            codeEntity.setClassEntity(classEntity); // Specific class
            codeEntity.setCodeHash(hash);
            codeEntity.setPlainCode(plain);
            codeEntity.setGeneratedBy(admin);
            codeEntity.setCreatedAt(Instant.now());
            codeEntity.setActive(true);
            
            codes.save(codeEntity);
            
            return ResponseEntity.ok(Map.of(
                "code", plain, 
                "codeId", codeEntity.getId(),
                "classId", classId,
                "className", classEntity.getClassName()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get active codes for a training module.
     * If classId is provided, returns code for that specific class.
     * Otherwise, returns codes for all classes.
     */
    @GetMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> getCodes(
            @PathVariable UUID trainingModuleId,
            @RequestParam(required = false) String classId) {
        try {
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            // If classId is provided, return code for specific class
            if (classId != null && !classId.isEmpty()) {
                UUID classUuid = UUID.fromString(classId);
                ClassEntity classEntity = classRepository.findById(classUuid)
                        .orElseThrow(() -> new RuntimeException("Class not found"));
                
                Optional<ModuleAccessCodeEntity> activeCodeOpt = codes.findFirstByTrainingModuleAndClassEntityAndIsActiveTrue(
                    trainingModule, classEntity
                );
                
                if (activeCodeOpt.isEmpty()) {
                    return ResponseEntity.ok(Map.of("hasCode", false));
                }
                
                ModuleAccessCodeEntity activeCode = activeCodeOpt.get();
                return buildCodeResponse(activeCode);
            }
            
            // Otherwise, return codes for all classes
            List<ModuleAccessCodeEntity> activeCodes = codes.findByTrainingModuleAndIsActiveTrue(trainingModule);
            
            List<Map<String, Object>> codesData = activeCodes.stream().map(code -> {
                Map<String, Object> codeMap = new LinkedHashMap<>();
                codeMap.put("codeId", code.getId());
                codeMap.put("code", code.getPlainCode());
                codeMap.put("classId", code.getClassEntity().getId());
                codeMap.put("className", code.getClassEntity().getClassName());
                codeMap.put("createdAt", code.getCreatedAt());
                codeMap.put("generatedBy", code.getGeneratedBy().getName());
                
                long usageCount = usageLogRepository.countByCodeId(code.getId());
                codeMap.put("usageCount", usageCount);
                
                return codeMap;
            }).toList();
            
            return ResponseEntity.ok(Map.of(
                "hasCode", !codesData.isEmpty(),
                "codes", codesData
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Verify an access code and unlock the training module for the student.
     * Student must be enrolled in the class that the code belongs to.
     */
    @PostMapping("/{trainingModuleId}/codes/verify")
    public ResponseEntity<?> verifyCode(
            @PathVariable UUID trainingModuleId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            String code = body.get("code");
            String classIdStr = body.get("classId");
            
            if (code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Code is required"));
            }
            
            if (classIdStr == null || classIdStr.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Class ID is required"));
            }
            
            UUID classId = UUID.fromString(classIdStr);
            
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));
            
            UserEntity user = users.findById(UUID.fromString(String.valueOf(auth.getName())))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify student is enrolled in this class
            Optional<ClassEnrollmentEntity> enrollment = enrollmentRepository.findByStudentAndClassEntity(user, classEntity);
            if (enrollment.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "You are not enrolled in this class"));
            }
            
            // Verify code
            String hash = codeUtils.sha256(code);
            Optional<ModuleAccessCodeEntity> codeOpt = codes.findFirstByTrainingModuleAndClassEntityAndCodeHashAndIsActiveTrue(
                trainingModule, classEntity, hash
            );
            
            if (codeOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or inactive code for this class"));
            }
            
            ModuleAccessCodeEntity codeEntity = codeOpt.get();
            
            // Log code usage (for statistics)
            CodeUsageLogEntity usageLog = new CodeUsageLogEntity();
            usageLog.setCodeId(codeEntity.getId());
            usageLog.setUser(user);
            usageLog.setCodeType("TRAINING_MODULE");
            usageLog.setResourceId(trainingModuleId);
            usageLogRepository.save(usageLog);
            
            // Unlock module by creating/updating progress
            ModuleProgressEntity progress = progressRepo.findByUserAndModule(user, trainingModule.getTraining())
                    .orElse(new ModuleProgressEntity());
            
            if (progress.getId() == null) {
                progress.setUser(user);
                progress.setModule(trainingModule.getTraining());
                progress.setStartedAt(Instant.now());
            }
            
            progress.setStatus("IN_PROGRESS");
            progressRepo.save(progress);
            
            return ResponseEntity.ok(Map.of(
                "unlocked", true,
                "message", "Training module unlocked successfully"
            ));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private ResponseEntity<?> buildCodeResponse(ModuleAccessCodeEntity activeCode) {
        long usageCount = usageLogRepository.countByCodeId(activeCode.getId());
        List<CodeUsageLogEntity> recentUsage = usageLogRepository.findByCodeIdOrderByUsedAtDesc(activeCode.getId());
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasCode", true);
        response.put("code", activeCode.getPlainCode());
        response.put("codeId", activeCode.getId());
        response.put("classId", activeCode.getClassEntity().getId());
        response.put("className", activeCode.getClassEntity().getClassName());
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
}



