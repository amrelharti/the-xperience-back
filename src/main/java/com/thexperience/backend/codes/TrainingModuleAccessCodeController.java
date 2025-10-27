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
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/training-modules")
public class TrainingModuleAccessCodeController {
    private final TrainingModuleRepository trainingModules;
    private final ModuleAccessCodeRepository codes;
    private final UserRepository users;
    private final CodeUtils codeUtils;

    public TrainingModuleAccessCodeController(
            TrainingModuleRepository trainingModules,
            ModuleAccessCodeRepository codes,
            UserRepository users,
            CodeUtils codeUtils) {
        this.trainingModules = trainingModules;
        this.codes = codes;
        this.users = users;
        this.codeUtils = codeUtils;
    }

    @PostMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> generateCodes(
            @PathVariable UUID trainingModuleId,
            @RequestBody Map<String, Integer> body,
            Authentication auth) {
        try {
            int count = Math.max(1, Math.min(50, body.getOrDefault("count", 1)));
            
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            // Get the parent training (ModuleEntity)
            ModuleEntity training = trainingModule.getTraining();
            
            UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName())))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            java.util.ArrayList<String> plainCodes = new java.util.ArrayList<>();
            
            for (int i = 0; i < count; i++) {
                String plain = codeUtils.generatePlainCode();
                String hash = codeUtils.sha256(plain);
                
                ModuleAccessCodeEntity codeEntity = new ModuleAccessCodeEntity();
                codeEntity.setModule(training); // Keep link to parent training for backward compatibility
                codeEntity.setTrainingModule(trainingModule); // CRITICAL FIX: Link to SPECIFIC training module
                codeEntity.setCodeHash(hash);
                codeEntity.setGeneratedBy(admin);
                codeEntity.setCreatedAt(Instant.now());
                
                codes.save(codeEntity);
                plainCodes.add(plain);
            }
            
            return ResponseEntity.ok(Map.of("codes", plainCodes));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{trainingModuleId}/codes")
    public ResponseEntity<?> listCodes(@PathVariable UUID trainingModuleId) {
        try {
            TrainingModuleEntity trainingModule = trainingModules.findById(trainingModuleId)
                    .orElseThrow(() -> new RuntimeException("Training module not found"));
            
            // Get codes for this specific training module
            List<Map<String, Object>> codesList = codes.findByTrainingModule(trainingModule).stream()
                    .map(c -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", c.getId());
                        m.put("isUsed", c.getConsumedAt() != null);
                        m.put("code", "••••••••"); // Hashed, can't display
                        m.put("createdAt", c.getCreatedAt());
                        m.put("consumedAt", c.getConsumedAt());
                        if (c.getConsumedBy() != null) {
                            Map<String, Object> user = new LinkedHashMap<>();
                            user.put("name", c.getConsumedBy().getName());
                            user.put("email", c.getConsumedBy().getEmail());
                            m.put("usedBy", user);
                        }
                        return m;
                    })
                    .toList();
            
            return ResponseEntity.ok(codesList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}



