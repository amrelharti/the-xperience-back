package com.thexperience.backend.courses;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CoursesController {
    private final CourseRepository courses;
    private final ModuleRepository modules;
    private final EnrollmentRepository enrollments;
    private final ModuleProgressRepository progressRepo;
    private final UserRepository users;

    public CoursesController(CourseRepository courses, ModuleRepository modules, EnrollmentRepository enrollments,
                             ModuleProgressRepository progressRepo, UserRepository users) {
        this.courses = courses; this.modules = modules; this.enrollments = enrollments; this.progressRepo = progressRepo; this.users = users;
    }

    @GetMapping("/courses")
    public List<Map<String,Object>> list(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        return courses.findAll().stream().map(c -> {
            List<ModuleEntity> mods = modules.findByCourse(c);
            long completed = mods.stream().filter(m -> progressRepo.findByUserAndModule(user, m).map(p -> "COMPLETED".equals(p.getStatus())).orElse(false)).count();
            Map<String,Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("title", c.getTitle());
            map.put("description", c.getDescription());
            map.put("thumbnailUrl", c.getThumbnailUrl());
            map.put("isPublished", c.isPublished());
            Map<String,Object> prog = new HashMap<>();
            prog.put("completedModules", completed);
            prog.put("totalModules", mods.size());
            prog.put("percent", mods.size() == 0 ? 0 : Math.round((completed * 100.0) / mods.size()));
            map.put("progress", prog);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/courses/{courseId}")
    public Map<String,Object> detail(@PathVariable UUID courseId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        CourseEntity c = courses.findById(courseId).orElseThrow();
        
        // Automatically enroll user if not already enrolled
        enrollments.findByUserAndCourse(user, c).orElseGet(() -> {
            EnrollmentEntity e = new EnrollmentEntity();
            e.setUser(user); 
            e.setCourse(c); 
            return enrollments.save(e);
        });
        
        List<ModuleEntity> mods = modules.findByCourse(c);
        List<Map<String,Object>> modDtos = new ArrayList<>();
        for (ModuleEntity m : mods) {
            String status = progressRepo.findByUserAndModule(user, m).map(ModuleProgressEntity::getStatus).orElse("LOCKED");
            Map<String,Object> mm = new HashMap<>();
            mm.put("id", m.getId());
            mm.put("title", m.getTitle());
            mm.put("durationSeconds", m.getDurationSeconds());
            mm.put("orderIndex", m.getOrderIndex());
            mm.put("status", status);
            modDtos.add(mm);
        }
        Map<String,Object> out = new HashMap<>();
        out.put("id", c.getId());
        out.put("title", c.getTitle());
        out.put("description", c.getDescription());
        out.put("thumbnailUrl", c.getThumbnailUrl());
        out.put("modules", modDtos);
        return out;
    }

    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<?> enroll(@PathVariable UUID courseId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        CourseEntity c = courses.findById(courseId).orElseThrow();
        enrollments.findByUserAndCourse(user, c).orElseGet(() -> {
            EnrollmentEntity e = new EnrollmentEntity();
            e.setUser(user); e.setCourse(c); return enrollments.save(e);
        });
        return ResponseEntity.ok(Map.of("enrolled", true));
    }

    @GetMapping("/modules/{moduleId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable UUID moduleId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        ModuleEntity m = modules.findById(moduleId).orElseThrow();
        ModuleProgressEntity mp = progressRepo.findByUserAndModule(user, m).orElse(null);
        if (mp == null) {
            return ResponseEntity.ok(Map.of("status", "LOCKED"));
        }
        return ResponseEntity.ok(Map.of("status", mp.getStatus()));
    }

    @PostMapping("/modules/{moduleId}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID moduleId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        UserEntity user = users.findById(userId).orElseThrow();
        ModuleEntity m = modules.findById(moduleId).orElseThrow();
        ModuleProgressEntity mp = progressRepo.findByUserAndModule(user, m).orElse(null);
        if (mp == null) { mp = new ModuleProgressEntity(); mp.setUser(user); mp.setModule(m); }
        mp.setStatus("COMPLETED"); mp.setCompletedAt(java.time.Instant.now());
        progressRepo.save(mp);
        return ResponseEntity.ok(Map.of("completed", true));
    }
}
