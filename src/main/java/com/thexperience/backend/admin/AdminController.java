package com.thexperience.backend.admin;

import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.repos.CourseRepository;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.repos.ClassRepository;
import com.thexperience.backend.repos.ClassEnrollmentRepository;
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
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    
    public AdminController(CourseRepository courses, ModuleRepository modules, ClassRepository classRepository, ClassEnrollmentRepository classEnrollmentRepository) {
        this.courses = courses;
        this.modules = modules;
        this.classRepository = classRepository;
        this.classEnrollmentRepository = classEnrollmentRepository;
    }
    
    // Module DTOs (Modules are now the top level)
    public record CreateModuleRequest(
        @NotBlank String title,
        @NotBlank String description,
        String thumbnailUrl,
        boolean isPublished,
        List<CourseRequest> courses
    ) {}
    
    public record CourseRequest(
        String title,
        String videoUrl
    ) {}
    
    public record UpdateModuleRequest(
        String title,
        String description,
        String thumbnailUrl,
        Boolean isPublished
    ) {}
    
    public record ModuleResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        boolean isPublished,
        Instant createdAt,
        Instant updatedAt,
        List<CourseResponse> courses
    ) {}
    
    // Course DTOs (Courses now belong to Modules)
    public record CreateCourseRequest(
        @NotNull UUID moduleId,
        @NotBlank String title,
        @NotBlank String description,
        String videoUrl,
        @NotNull Integer durationSeconds,
        @NotNull Integer orderIndex
    ) {}
    
    public record UpdateCourseRequest(
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex
    ) {}
    
    public record CourseResponse(
        UUID id,
        UUID moduleId,
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    // Class DTOs
    public record ClassResponse(
        UUID id,
        String name,
        String description,
        int studentCount,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    // Module CRUD Operations
    
    @GetMapping("/modules")
    public List<ModuleResponse> listModules() {
        return modules.findAll().stream()
            .map(module -> {
                List<CourseResponse> moduleCourses = courses.findByModuleOrderByOrderIndexAsc(module)
                    .stream()
                    .map(this::toCourseResponse)
                    .toList();
                return toModuleResponse(module, moduleCourses);
            })
            .toList();
    }
    
    @GetMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> getModuleById(@PathVariable UUID id) {
        return modules.findById(id)
            .map(module -> {
                List<CourseResponse> moduleCourses = courses.findByModuleOrderByOrderIndexAsc(module)
                    .stream()
                    .map(this::toCourseResponse)
                    .toList();
                return toModuleResponse(module, moduleCourses);
            })
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/modules")
    public ResponseEntity<ModuleResponse> createModuleNew(@Valid @RequestBody CreateModuleRequest request) {
        try {
            ModuleEntity module = new ModuleEntity();
            module.setTitle(request.title());
            module.setDescription(request.description());
            module.setThumbnailUrl(request.thumbnailUrl());
            module.setPublished(request.isPublished());
            module.setCreatedAt(Instant.now());
            module.setUpdatedAt(Instant.now());
            
            ModuleEntity saved = modules.save(module);
            
            // Create courses if provided
            if (request.courses() != null && !request.courses().isEmpty()) {
                for (CourseRequest courseRequest : request.courses()) {
                    if (courseRequest.title() != null && !courseRequest.title().trim().isEmpty()) {
                        CourseEntity course = new CourseEntity();
                        course.setTitle(courseRequest.title());
                        course.setDescription(""); // Default empty description
                        course.setVideoUrl(courseRequest.videoUrl() != null ? courseRequest.videoUrl() : "");
                        course.setDurationSeconds(0); // Default duration
                        course.setOrderIndex(0); // Default order
                        course.setModule(saved);
                        course.setCreatedAt(Instant.now());
                        course.setUpdatedAt(Instant.now());
                        courses.save(course);
                    }
                }
            }
            
            return ResponseEntity.ok(toModuleResponse(saved));
        } catch (Exception e) {
            System.out.println("Error creating module with courses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/modules/{id}")
    public ResponseEntity<ModuleResponse> updateModuleById(@PathVariable UUID id, @Valid @RequestBody UpdateModuleRequest request) {
        try {
            ModuleEntity module = modules.findById(id).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (request.title() != null) module.setTitle(request.title());
            if (request.description() != null) module.setDescription(request.description());
            if (request.thumbnailUrl() != null) module.setThumbnailUrl(request.thumbnailUrl());
            if (request.isPublished() != null) module.setPublished(request.isPublished());
            module.setUpdatedAt(Instant.now());
            
            ModuleEntity saved = modules.save(module);
            return ResponseEntity.ok(toModuleResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModuleById(@PathVariable UUID id) {
        try {
            if (modules.existsById(id)) {
                modules.deleteById(id);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Legacy endpoints (keeping for backward compatibility)
    @GetMapping("/courses")
    public List<ModuleResponse> listModulesLegacy() {
        return modules.findAll().stream()
            .map(module -> {
                List<CourseResponse> moduleCourses = courses.findByModuleOrderByOrderIndexAsc(module)
                    .stream()
                    .map(this::toCourseResponse)
                    .toList();
                return toModuleResponse(module, moduleCourses);
            })
            .toList();
    }
    
    @GetMapping("/courses/{id}")
    public ResponseEntity<ModuleResponse> getModule(@PathVariable UUID id) {
        return modules.findById(id)
            .map(module -> {
                List<CourseResponse> moduleCourses = courses.findByModuleOrderByOrderIndexAsc(module)
                    .stream()
                    .map(this::toCourseResponse)
                    .toList();
                return toModuleResponse(module, moduleCourses);
            })
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/courses")
    public ResponseEntity<ModuleResponse> createModule(@Valid @RequestBody CreateModuleRequest request) {
        try {
            ModuleEntity module = new ModuleEntity();
            module.setTitle(request.title());
            module.setDescription(request.description());
            module.setThumbnailUrl(request.thumbnailUrl());
            module.setPublished(request.isPublished());
            module.setCreatedAt(Instant.now());
            module.setUpdatedAt(Instant.now());
            
            ModuleEntity saved = modules.save(module);
            return ResponseEntity.ok(toModuleResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/courses/{id}")
    public ResponseEntity<ModuleResponse> updateModule(@PathVariable UUID id, @Valid @RequestBody UpdateModuleRequest request) {
        try {
            ModuleEntity module = modules.findById(id).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (request.title() != null) module.setTitle(request.title());
            if (request.description() != null) module.setDescription(request.description());
            if (request.thumbnailUrl() != null) module.setThumbnailUrl(request.thumbnailUrl());
            if (request.isPublished() != null) module.setPublished(request.isPublished());
            module.setUpdatedAt(Instant.now());
            
            ModuleEntity saved = modules.save(module);
            return ResponseEntity.ok(toModuleResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    
    // Course CRUD Operations
    
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
    
    @PostMapping("/courses/{moduleId}/modules")
    public ResponseEntity<CourseResponse> createCourse(@PathVariable UUID moduleId, @Valid @RequestBody CreateCourseRequest request) {
        try {
            ModuleEntity module = modules.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }
            
            CourseEntity course = new CourseEntity();
            course.setModule(module);
            course.setTitle(request.title());
            course.setDescription(request.description());
            course.setVideoUrl(request.videoUrl());
            course.setDurationSeconds(request.durationSeconds());
            course.setOrderIndex(request.orderIndex());
            course.setCreatedAt(Instant.now());
            course.setUpdatedAt(Instant.now());
            
            CourseEntity saved = courses.save(course);
            return ResponseEntity.ok(toCourseResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/courses/{moduleId}/modules/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable UUID moduleId, @PathVariable UUID courseId, @Valid @RequestBody UpdateCourseRequest request) {
        try {
            CourseEntity course = courses.findById(courseId).orElse(null);
            if (course == null || !course.getModule().getId().equals(moduleId)) {
                return ResponseEntity.notFound().build();
            }
            
            if (request.title() != null) course.setTitle(request.title());
            if (request.description() != null) course.setDescription(request.description());
            if (request.videoUrl() != null) course.setVideoUrl(request.videoUrl());
            if (request.durationSeconds() != null) course.setDurationSeconds(request.durationSeconds());
            if (request.orderIndex() != null) course.setOrderIndex(request.orderIndex());
            course.setUpdatedAt(Instant.now());
            
            CourseEntity saved = courses.save(course);
            return ResponseEntity.ok(toCourseResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteModule(@PathVariable UUID id) {
        try {
            ModuleEntity module = modules.findById(id).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }
            
            // With cascade configuration, JPA will automatically handle deletion of:
            // - Training modules (trainingModules)
            // - Courses (courses)
            // - And all their related entities
            
            // Delete the module - cascade will handle the rest
            modules.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle foreign key constraints
            System.err.println("Error deleting training: " + e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Cannot delete training. It may have associated data that cannot be removed."));
        } catch (Exception e) {
            System.err.println("Unexpected error deleting training: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete training"));
        }
    }
    
    @DeleteMapping("/courses/{moduleId}/modules/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID moduleId, @PathVariable UUID courseId) {
        try {
            CourseEntity course = courses.findById(courseId).orElse(null);
            if (course != null && course.getModule().getId().equals(moduleId)) {
                courses.deleteById(courseId);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Class Management Operations
    
    @GetMapping("/classes")
    public List<ClassResponse> listClasses() {
        return classRepository.findAll().stream()
            .map(this::toClassResponse)
            .toList();
    }
    
    // Helper methods
    
    private ModuleResponse toModuleResponse(ModuleEntity module) {
        return toModuleResponse(module, List.of());
    }
    
    private ModuleResponse toModuleResponse(ModuleEntity module, List<CourseResponse> courses) {
        return new ModuleResponse(
            module.getId(),
            module.getTitle(),
            module.getDescription(),
            module.getThumbnailUrl(),
            module.isPublished(),
            module.getCreatedAt(),
            module.getUpdatedAt(),
            courses
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
            course.getCreatedAt(),
            course.getUpdatedAt()
        );
    }
    
    private ClassResponse toClassResponse(ClassEntity classEntity) {
        int studentCount = classEnrollmentRepository.findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classEntity).size();
        return new ClassResponse(
            classEntity.getId(),
            classEntity.getName(),
            classEntity.getDescription(),
            studentCount,
            classEntity.getCreatedAt(),
            classEntity.getUpdatedAt()
        );
    }
}