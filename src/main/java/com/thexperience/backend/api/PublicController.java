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
    public record CourseResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        boolean isPublished,
        String createdAt,
        String updatedAt
    ) {}
    
    public record ModuleResponse(
        UUID id,
        UUID courseId,
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex,
        String createdAt,
        String updatedAt
    ) {}
    
    // Public Course Endpoints
    
    @GetMapping("/courses")
    public List<CourseResponse> listCourses() {
        return courses.findByIsPublishedTrue().stream()
            .map(this::toCourseResponse)
            .toList();
    }
    
    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> getCourse(@PathVariable UUID id) {
        return courses.findById(id)
            .filter(CourseEntity::isPublished)
            .map(this::toCourseResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/courses/{courseId}/modules")
    public List<ModuleResponse> listModules(@PathVariable UUID courseId) {
        return modules.findByCourseId(courseId).stream()
            .map(this::toModuleResponse)
            .toList();
    }
    
    @GetMapping("/courses/{courseId}/modules/{moduleId}")
    public ResponseEntity<ModuleResponse> getModule(@PathVariable UUID courseId, @PathVariable UUID moduleId) {
        return modules.findById(moduleId)
            .filter(module -> module.getCourse().getId().equals(courseId))
            .map(this::toModuleResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Helper methods
    
    private CourseResponse toCourseResponse(CourseEntity course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getThumbnailUrl(),
            course.isPublished(),
            course.getCreatedAt().toString(),
            course.getUpdatedAt().toString()
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
            module.getCreatedAt().toString(),
            module.getUpdatedAt().toString()
        );
    }
}
