package com.thexperience.backend.api;

import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.repos.CourseRepository;
import com.thexperience.backend.repos.ModuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    
    private final CourseRepository courses;
    private final ModuleRepository modules;
    
    public PublicController(CourseRepository courses, ModuleRepository modules) {
        this.courses = courses;
        this.modules = modules;
    }
    
    // Public DTOs
    public record ModuleResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        boolean isPublished,
        String createdAt,
        String updatedAt
    ) {}
    
    public record CourseResponse(
        UUID id,
        UUID moduleId,
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex,
        String createdAt,
        String updatedAt
    ) {}
    
    // Public Module Endpoints (Modules are now the top level)
    
    @GetMapping("/courses")
    public List<ModuleResponse> listModules() {
        return modules.findAll().stream()
            .map(this::toModuleResponse)
            .toList();
    }
    
    @GetMapping("/courses/{id}")
    public ResponseEntity<ModuleResponse> getModule(@PathVariable UUID id) {
        return modules.findById(id)
            .filter(ModuleEntity::isPublished)
            .map(this::toModuleResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/courses/{moduleId}/modules")
    public List<CourseResponse> listCourses(@PathVariable UUID moduleId) {
        return modules.findById(moduleId)
            .map(module -> courses.findByModuleOrderByOrderIndexAsc(module))
            .orElse(List.of())
            .stream()
            .map(this::toCourseResponse)
            .toList();
    }
    
    @GetMapping("/courses/{moduleId}/modules/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID moduleId, @PathVariable UUID courseId) {
        return courses.findById(courseId)
            .filter(course -> course.getModule().getId().equals(moduleId))
            .map(this::toCourseResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Helper methods
    
    private ModuleResponse toModuleResponse(ModuleEntity module) {
        return new ModuleResponse(
            module.getId(),
            module.getTitle(),
            module.getDescription(),
            module.getThumbnailUrl(),
            module.isPublished(),
            module.getCreatedAt().toString(),
            module.getUpdatedAt().toString()
        );
    }
    
    private CourseResponse toCourseResponse(CourseEntity course) {
        return new CourseResponse(
            course.getId(),
            course.getModule().getId(),
            course.getTitle(),
            course.getDescription(),
            course.getVideoUrl(),
            course.getDurationSeconds(),
            course.getOrderIndex(),
            course.getCreatedAt().toString(),
            course.getUpdatedAt().toString()
        );
    }
}
