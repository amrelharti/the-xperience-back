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
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ClassEnrollmentCodeController {
    private final ClassRepository classRepository;
    private final ClassEnrollmentCodeRepository codes;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final UserRepository users;
    private final CodeUtils codeUtils;

    public ClassEnrollmentCodeController(ClassRepository classRepository, ClassEnrollmentCodeRepository codes,
                                         ClassEnrollmentRepository enrollmentRepository, UserRepository users, CodeUtils codeUtils) {
        this.classRepository = classRepository;
        this.codes = codes;
        this.enrollmentRepository = enrollmentRepository;
        this.users = users;
        this.codeUtils = codeUtils;
    }

    @PostMapping("/api/admin/classes/{classId}/codes")
    public ResponseEntity<?> generate(@PathVariable UUID classId, @RequestBody Map<String, Integer> body, Authentication auth) {
        int count = Math.max(1, Math.min(50, body.getOrDefault("count", 1)));
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        UserEntity admin = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        java.util.ArrayList<String> plainCodes = new java.util.ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String plain = codeUtils.generatePlainCode();
            String hash = codeUtils.sha256(plain);
            ClassEnrollmentCodeEntity e = new ClassEnrollmentCodeEntity();
            e.setClassEntity(classEntity);
            e.setCodeHash(hash);
            e.setGeneratedBy(admin);
            codes.save(e);
            plainCodes.add(plain);
        }
        return ResponseEntity.ok(Map.of("codes", plainCodes));
    }

    @GetMapping("/api/admin/classes/{classId}/codes")
    public List<Map<String, Object>> list(@PathVariable UUID classId) {
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
        return codes.findByClassEntity(classEntity).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("used", c.getConsumedAt() != null);
                    m.put("createdAt", c.getCreatedAt());
                    m.put("consumedAt", c.getConsumedAt());
                    return m;
                })
                .toList();
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
        ClassEnrollmentCodeEntity c = codes.findFirstByClassEntityAndCodeHashAndConsumedAtIsNull(classEntity, hash).orElse(null);
        
        if (c == null) return ResponseEntity.status(400).body(Map.of("error","Invalid or used code"));
        
        UserEntity user = users.findById(UUID.fromString(String.valueOf(auth.getName()))).orElseThrow();
        
        // Check if user is already enrolled in this class
        boolean alreadyEnrolled = enrollmentRepository.findByStudentAndClassEntity(user, classEntity).isPresent();
        if (alreadyEnrolled) {
            return ResponseEntity.ok(Map.of("enrolled", true, "message", "You are already enrolled in this class"));
        }
        
        // Mark code as consumed
        c.setConsumedAt(Instant.now());
        c.setConsumedBy(user);
        codes.save(c);
        
        // Create enrollment
        ClassEnrollmentEntity enrollment = new ClassEnrollmentEntity(user, classEntity);
        enrollmentRepository.save(enrollment);
        
        return ResponseEntity.ok(Map.of("enrolled", true, "message", "Successfully enrolled in class"));
    }
}


