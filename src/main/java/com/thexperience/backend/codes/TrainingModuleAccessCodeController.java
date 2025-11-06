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
@RequestMapping("/api/admin/training-modules")
public class TrainingModuleAccessCodeController {
    private final TrainingModuleRepository trainingModules;
    private final ModuleAccessCodeRepository codes;
    private final UserRepository users;
    private final CodeUtils codeUtils;
    private final CodeUsageLogRepository usageLogRepository;

    public TrainingModuleAccessCodeController(
            TrainingModuleRepository trainingModules,
            ModuleAccessCodeRepository codes,
            UserRepository users,
            CodeUtils codeUtils,
            CodeUsageLogRepository usageLogRepository) {
        this.trainingModules = trainingModules;
        this.codes = codes;
        this.users = users;
        this.codeUtils = codeUtils;
        this.usageLogRepository = usageLogRepository;
    }

    @PostMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> generateCode(
            @PathVariable UUID trainingModuleId,
            @RequestBody Map<String, Integer> body,
            Authentication auth) {
        try {
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            // Get the parent training (ModuleEntity)
            ModuleEntity training = trainingModule.getTraining();
            
            UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName())))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Deactivate all existing codes for this training module
            List<ModuleAccessCodeEntity> existingCodes = codes.findByTrainingModule(trainingModule);
            for (ModuleAccessCodeEntity code : existingCodes) {
                code.setActive(false);
                codes.save(code);
            }
            
            // Generate ONE new active code (multi-use by default)
            String plain = codeUtils.generatePlainCode();
            String hash = codeUtils.sha256(plain);
            
            ModuleAccessCodeEntity codeEntity = new ModuleAccessCodeEntity();
            codeEntity.setModule(training); // Keep link to parent training for backward compatibility
            codeEntity.setTrainingModule(trainingModule); // CRITICAL: Link to SPECIFIC training module
            codeEntity.setCodeHash(hash);
            codeEntity.setPlainCode(plain); // Store plain code so admin can view it later
            codeEntity.setGeneratedBy(admin);
            codeEntity.setCreatedAt(Instant.now());
            codeEntity.setActive(true);
            codeEntity.setSingleUse(false); // CRITICAL FIX: Set to multi-use!
            
            codes.save(codeEntity);
            
            return ResponseEntity.ok(Map.of("code", plain, "codeId", codeEntity.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> getActiveCode(@PathVariable UUID trainingModuleId) {
        try {
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            // Get the active code for this specific training module
            Optional<ModuleAccessCodeEntity> activeCodeOpt = codes.findFirstByTrainingModuleAndIsActiveTrue(trainingModule);
            
            if (activeCodeOpt.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasCode", false));
            }
            
            ModuleAccessCodeEntity activeCode = activeCodeOpt.get();
            
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
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



