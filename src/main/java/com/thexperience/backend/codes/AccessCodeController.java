package com.thexperience.backend.codes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class AccessCodeController {
    private final ModuleRepository modules;
    private final ModuleAccessCodeRepository codes;
    private final UserRepository users;
    private final ModuleProgressRepository progressRepo;
    private final CodeUtils codeUtils;

    public AccessCodeController(ModuleRepository modules, ModuleAccessCodeRepository codes, UserRepository users,
                                ModuleProgressRepository progressRepo, CodeUtils codeUtils) {
        this.modules = modules; this.codes = codes; this.users = users; this.progressRepo = progressRepo; this.codeUtils = codeUtils;
    }

    @PostMapping("/api/admin/modules/{moduleId}/codes")
    public ResponseEntity<?> generate(@PathVariable UUID moduleId, @RequestBody Map<String, Integer> body, Authentication auth) {
        int count = Math.max(1, Math.min(50, body.getOrDefault("count", 1)));
        ModuleEntity module = modules.findById(moduleId).orElseThrow();
        UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        java.util.ArrayList<String> plainCodes = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String plain = codeUtils.generatePlainCode();
            String hash = codeUtils.sha256(plain);
            ModuleAccessCodeEntity e = new ModuleAccessCodeEntity();
            e.setModule(module);
            e.setCodeHash(hash);
            e.setGeneratedBy(admin);
            codes.save(e);
            plainCodes.add(plain);
        }
        return ResponseEntity.ok(Map.of("codes", plainCodes));
    }

    @GetMapping("/api/admin/modules/{moduleId}/codes")
    public List<Map<String,Object>> list(@PathVariable UUID moduleId) {
        ModuleEntity module = modules.findById(moduleId).orElseThrow();
        return codes.findByModule(module).stream().map(c -> Map.of(
                "id", c.getId(),
                "used", c.getConsumedAt() != null,
                "createdAt", c.getCreatedAt(),
                "consumedAt", c.getConsumedAt()
        )).toList();
    }

    @DeleteMapping("/api/admin/codes/{codeId}")
    public ResponseEntity<?> delete(@PathVariable UUID codeId) {
        ModuleAccessCodeEntity c = codes.findById(codeId).orElseThrow();
        if (c.getConsumedAt() != null) return ResponseEntity.badRequest().body(Map.of("error","Code already used"));
        codes.delete(c);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/modules/{moduleId}/codes/verify")
    public ResponseEntity<?> verify(@PathVariable UUID moduleId, @RequestBody Map<String,String> body, Authentication auth) {
        String code = body.getOrDefault("code", "");
        if (code.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","Code required"));
        ModuleEntity module = modules.findById(moduleId).orElseThrow();
        String hash = codeUtils.sha256(code);
        ModuleAccessCodeEntity c = codes.findFirstByModuleAndCodeHashAndConsumedAtIsNull(module, hash).orElse(null);
        if (c == null) return ResponseEntity.status(400).body(Map.of("error","Invalid or used code"));
        UserEntity user = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        c.setConsumedAt(Instant.now());
        c.setConsumedBy(user);
        codes.save(c);
        ModuleProgressEntity mp = progressRepo.findByUserAndModule(user, module).orElse(null);
        if (mp == null) { mp = new ModuleProgressEntity(); mp.setUser(user); mp.setModule(module); }
        mp.setStatus("IN_PROGRESS"); mp.setStartedAt(Instant.now());
        progressRepo.save(mp);
        return ResponseEntity.ok(Map.of("unlocked", true));
    }
}
