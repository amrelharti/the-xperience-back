package com.thexperience.backend.admin;

import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.repos.CourseRepository;
import com.thexperience.backend.repos.ModuleRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    private final CourseRepository courses;
    private final ModuleRepository modules;
    
    public AdminController(CourseRepository courses, ModuleRepository modules) {
        this.courses = courses;
        this.modules = modules;
    }
    
    // Course DTOs
    public record CreateCourseRequest(
        @NotBlank String title,
        @NotBlank String description,
        String thumbnailUrl,
        boolean isPublished
    ) {}
    
    public record UpdateCourseRequest(
        String title,
        String description,
        String thumbnailUrl,
        Boolean isPublished
    ) {}
    
    public record CourseResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        boolean isPublished,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    // Module DTOs
    public record CreateModuleRequest(
        @NotNull UUID courseId,
        @NotBlank String title,
        @NotBlank String description,
        String videoUrl,
        @NotNull Integer durationSeconds,
        @NotNull Integer orderIndex
    ) {}
    
    public record UpdateModuleRequest(
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex
    ) {}
    
    public record ModuleResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    // Course CRUD Operations
    
    @GetMapping("/courses")
    public List<CourseResponse> listCourses() {
        return courses.findAll().stream()
            .map(this::toCourseResponse)
            .toList();
    }
    
    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID id) {
        return courses.findById(id)
            .map(this::toCourseResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseEntity course = new CourseEntity();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setThumbnailUrl(request.thumbnailUrl());
        course.setPublished(request.isPublished());
        course.setCreatedAt(Instant.now());
        course.setUpdatedAt(Instant.now());
        
        CourseEntity saved = courses.save(course);
        return ResponseEntity.ok(toCourseResponse(saved));
    }
    
    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable UUID id, @Valid @RequestBody UpdateCourseRequest request) {
        return courses.findById(id)
            .map(course -> {
                if (request.title() != null) course.setTitle(request.title());
                if (request.description() != null) course.setDescription(request.description());
                if (request.thumbnailUrl() != null) course.setThumbnailUrl(request.thumbnailUrl());
                if (request.isPublished() != null) course.setPublished(request.isPublished());
                course.setUpdatedAt(Instant.now());
                
                CourseEntity saved = courses.save(course);
                return ResponseEntity.ok(toCourseResponse(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable UUID id) {
        if (!courses.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Delete all modules for this course first
        modules.findByCourseId(id).forEach(modules::delete);
        
        // Then delete the course
        courses.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // Module CRUD Operations
    
    @GetMapping("/courses/{courseId}/modules")
    public List<ModuleResponse> listModules(@PathVariable UUID courseId) {
        return modules.findByCourseId(courseId).stream()
            .map(this::toModuleResponse)
            .toList();
    }
    
    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> getModule(@PathVariable UUID id) {
        return modules.findById(id)
            .map(this::toModuleResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/modules")
    public ResponseEntity<ModuleResponse> createModule(@Valid @RequestBody CreateModuleRequest request) {
        CourseEntity course = courses.findById(request.courseId())
            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        
        ModuleEntity module = new ModuleEntity();
        module.setCourse(course);
        module.setTitle(request.title());
        module.setDescription(request.description());
        module.setVideoUrl(request.videoUrl());
        module.setDurationSeconds(request.durationSeconds());
        module.setOrderIndex(request.orderIndex());
        module.setCreatedAt(Instant.now());
        module.setUpdatedAt(Instant.now());
        
        ModuleEntity saved = modules.save(module);
        return ResponseEntity.ok(toModuleResponse(saved));
    }
    
    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> updateModule(@PathVariable UUID id, @Valid @RequestBody UpdateModuleRequest request) {
        return modules.findById(id)
            .map(module -> {
                if (request.title() != null) module.setTitle(request.title());
                if (request.description() != null) module.setDescription(request.description());
                if (request.videoUrl() != null) module.setVideoUrl(request.videoUrl());
                if (request.durationSeconds() != null) module.setDurationSeconds(request.durationSeconds());
                if (request.orderIndex() != null) module.setOrderIndex(request.orderIndex());
                module.setUpdatedAt(Instant.now());
                
                ModuleEntity saved = modules.save(module);
                return ResponseEntity.ok(toModuleResponse(saved));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable UUID id) {
        if (!modules.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        modules.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    // Helper methods
    
    private CourseResponse toCourseResponse(CourseEntity course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getThumbnailUrl(),
            course.isPublished(),
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }
    
    private ModuleResponse toModuleResponse(ModuleEntity module) {
        return new ModuleResponse(
            module.getId(),
            module.getCourse().getId(),
            module.getTitle(),
            module.getDescription(),
            module.getVideoUrl(),
            module.getDurationSeconds(),
            module.getOrderIndex(),
            module.getCreatedAt(),
            module.getUpdatedAt()
        );
    }
}
