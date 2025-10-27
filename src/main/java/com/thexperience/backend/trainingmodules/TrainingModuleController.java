package com.thexperience.backend.trainingmodules;

import com.thexperience.backend.entities.ModuleEntity;
import com.thexperience.backend.entities.TrainingModuleEntity;
import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.repos.TrainingModuleRepository;
import com.thexperience.backend.repos.CourseRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for managing TrainingModules (Modules/Weeks within a Training)
 * Hierarchy: Training → TrainingModule → Course
 * All classes in a training share the same modules (curriculum)
 */
@RestController
@RequestMapping("/api/admin")
public class TrainingModuleController {

    private final TrainingModuleRepository trainingModuleRepository;
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;

    public TrainingModuleController(
        TrainingModuleRepository trainingModuleRepository,
        ModuleRepository moduleRepository,
        CourseRepository courseRepository
    ) {
        this.trainingModuleRepository = trainingModuleRepository;
        this.moduleRepository = moduleRepository;
        this.courseRepository = courseRepository;
    }

    // DTOs
    public record CreateTrainingModuleRequest(
        @NotNull UUID trainingId,
        @NotBlank String title,
        String description,
        @NotNull Integer orderIndex,
        String thumbnailUrl
    ) {}

    public record UpdateTrainingModuleRequest(
        String title,
        String description,
        Integer orderIndex,
        Boolean isActive,
        String thumbnailUrl
    ) {}

    public record TrainingModuleResponse(
        UUID id,
        UUID trainingId,
        String title,
        String description,
        String thumbnailUrl,
        Integer orderIndex,
        Boolean isActive,
        Integer coursesCount,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record CreateCourseRequest(
        String title,
        String description,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex
    ) {}

    public record CourseResponse(
        UUID id,
        String title,
        String description,
        String thumbnailUrl,
        String videoUrl,
        Integer durationSeconds,
        Integer orderIndex
    ) {}

    // ============ ENDPOINTS ============

    /**
     * GET /api/admin/courses/{trainingId}/training-modules
     * Get all modules for a specific training
     */
    @GetMapping("/courses/{trainingId}/training-modules")
    public ResponseEntity<List<TrainingModuleResponse>> getModulesByTraining(@PathVariable UUID trainingId) {
        try {
            ModuleEntity training = moduleRepository.findById(trainingId).orElse(null);
            if (training == null) {
                return ResponseEntity.notFound().build();
            }

            List<TrainingModuleEntity> modules = trainingModuleRepository
                .findByTrainingOrderByOrderIndexAsc(training);

            List<TrainingModuleResponse> response = modules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/admin/training-modules/{moduleId}
     * Get specific module details
     */
    @GetMapping("/training-modules/{moduleId}")
    public ResponseEntity<TrainingModuleResponse> getModule(@PathVariable UUID moduleId) {
        return trainingModuleRepository.findById(moduleId)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/admin/courses/{trainingId}/training-modules
     * Create a new module within a training
     */
    @PostMapping("/courses/{trainingId}/training-modules")
    public ResponseEntity<TrainingModuleResponse> createModule(
        @PathVariable UUID trainingId,
        @Valid @RequestBody CreateTrainingModuleRequest request
    ) {
        try {
            ModuleEntity training = moduleRepository.findById(trainingId).orElse(null);
            if (training == null) {
                return ResponseEntity.notFound().build();
            }

            TrainingModuleEntity module = new TrainingModuleEntity();
            module.setTraining(training);
            module.setTitle(request.title());
            module.setDescription(request.description());
            module.setThumbnailUrl(request.thumbnailUrl());
            module.setOrderIndex(request.orderIndex());
            module.setIsActive(true);
            module.setCreatedAt(Instant.now());
            module.setUpdatedAt(Instant.now());

            TrainingModuleEntity saved = trainingModuleRepository.save(module);
            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/admin/training-modules/{moduleId}
     * Update a module
     */
    @PutMapping("/training-modules/{moduleId}")
    public ResponseEntity<TrainingModuleResponse> updateModule(
        @PathVariable UUID moduleId,
        @Valid @RequestBody UpdateTrainingModuleRequest request
    ) {
        try {
            TrainingModuleEntity module = trainingModuleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }

            if (request.title() != null) module.setTitle(request.title());
            if (request.description() != null) module.setDescription(request.description());
            if (request.thumbnailUrl() != null) module.setThumbnailUrl(request.thumbnailUrl());
            if (request.orderIndex() != null) module.setOrderIndex(request.orderIndex());
            if (request.isActive() != null) module.setIsActive(request.isActive());
            module.setUpdatedAt(Instant.now());

            TrainingModuleEntity saved = trainingModuleRepository.save(module);
            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/admin/training-modules/{moduleId}
     * Delete a module
     */
    @DeleteMapping("/training-modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@PathVariable UUID moduleId) {
        try {
            if (!trainingModuleRepository.existsById(moduleId)) {
                return ResponseEntity.notFound().build();
            }
            trainingModuleRepository.deleteById(moduleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/admin/training-modules/{moduleId}/courses
     * Get all courses within a module
     */
    @GetMapping("/training-modules/{moduleId}/courses")
    public ResponseEntity<List<CourseResponse>> getCoursesByModule(@PathVariable UUID moduleId) {
        try {
            TrainingModuleEntity module = trainingModuleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }

            List<CourseEntity> courses = module.getCourses();
            List<CourseResponse> response = courses.stream()
                .map(this::toCourseResponse)
                .sorted((a, b) -> a.orderIndex().compareTo(b.orderIndex()))
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/admin/training-modules/{moduleId}/courses
     * Create a new course within a training module
     */
    @PostMapping("/training-modules/{moduleId}/courses")
    public ResponseEntity<CourseResponse> createCourse(
        @PathVariable UUID moduleId,
        @RequestBody CreateCourseRequest request
    ) {
        try {
            TrainingModuleEntity module = trainingModuleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }

            CourseEntity course = new CourseEntity();
            course.setTrainingModule(module);
            course.setModule(module.getTraining()); // Set the old module_id for backward compatibility
            course.setTitle(request.title());
            course.setDescription(request.description());
            course.setVideoUrl(request.videoUrl());
            course.setDurationSeconds(request.durationSeconds());
            course.setOrderIndex(request.orderIndex());
            course.setThumbnailUrl(null); // Courses don't have thumbnails
            course.setCreatedAt(Instant.now());
            course.setUpdatedAt(Instant.now());

            CourseEntity saved = courseRepository.save(course);
            return ResponseEntity.ok(toCourseResponse(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/admin/training-modules/{moduleId}/courses/{courseId}
     * Delete a course within a training module
     */
    @DeleteMapping("/training-modules/{moduleId}/courses/{courseId}")
    public ResponseEntity<Void> deleteCourse(
        @PathVariable UUID moduleId,
        @PathVariable UUID courseId
    ) {
        try {
            TrainingModuleEntity module = trainingModuleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }

            CourseEntity course = courseRepository.findById(courseId).orElse(null);
            if (course == null || !course.getTrainingModule().getId().equals(moduleId)) {
                return ResponseEntity.notFound().build();
            }

            courseRepository.deleteById(courseId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/admin/training-modules/{moduleId}/courses/{courseId}
     * Update a course within a training module
     */
    @PutMapping("/training-modules/{moduleId}/courses/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(
        @PathVariable UUID moduleId,
        @PathVariable UUID courseId,
        @RequestBody CreateCourseRequest request
    ) {
        try {
            TrainingModuleEntity module = trainingModuleRepository.findById(moduleId).orElse(null);
            if (module == null) {
                return ResponseEntity.notFound().build();
            }

            CourseEntity course = courseRepository.findById(courseId).orElse(null);
            if (course == null || !course.getTrainingModule().getId().equals(moduleId)) {
                return ResponseEntity.notFound().build();
            }

            course.setTitle(request.title());
            course.setDescription(request.description());
            course.setVideoUrl(request.videoUrl());
            course.setDurationSeconds(request.durationSeconds());
            course.setOrderIndex(request.orderIndex());
            course.setUpdatedAt(Instant.now());

            CourseEntity saved = courseRepository.save(course);
            return ResponseEntity.ok(toCourseResponse(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    // ============ HELPER METHODS ============

    private TrainingModuleResponse toResponse(TrainingModuleEntity module) {
        return new TrainingModuleResponse(
            module.getId(),
            module.getTraining().getId(),
            module.getTitle(),
            module.getDescription(),
            module.getThumbnailUrl(),
            module.getOrderIndex(),
            module.getIsActive(),
            module.getCourses() != null ? module.getCourses().size() : 0,
            module.getCreatedAt(),
            module.getUpdatedAt()
        );
    }

    private CourseResponse toCourseResponse(CourseEntity course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getThumbnailUrl(),
            course.getVideoUrl(),
            course.getDurationSeconds(),
            course.getOrderIndex()
        );
    }
}

