package com.thexperience.backend.classes;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.ClassRepository;
import com.thexperience.backend.repos.ClassEnrollmentRepository;
import com.thexperience.backend.repos.ModuleRepository;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.classes.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class ClassController {

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private UserRepository userRepository;

    // Get all classes
    @GetMapping
    public ResponseEntity<?> getAllClasses() {
        try {
            List<ClassEntity> classes = classRepository.findByIsArchivedFalseOrderByStartDateDesc();
            List<ClassResponse> responses = classes.stream()
                    .map(this::convertClassToResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching classes: " + e.getMessage());
        }
    }

    // Create a new class (Public for testing)
    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody CreateClassRequest request) {
        try {
            Optional<ModuleEntity> moduleOpt = moduleRepository.findById(request.getModuleId());
            if (moduleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Module not found");
            }

            ClassEntity classEntity = new ClassEntity();
            classEntity.setName(request.getName());
            classEntity.setDescription(request.getDescription());
            classEntity.setModule(moduleOpt.get());
            classEntity.setCreatedBy(null); // No user for testing
            classEntity.setStartDate(request.getStartDate());
            classEntity.setEndDate(request.getEndDate());
            classEntity.setMaxStudents(request.getMaxStudents());

            classEntity = classRepository.save(classEntity);

            return ResponseEntity.ok(convertClassToResponse(classEntity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating class: " + e.getMessage());
        }
    }

    // Get all classes for a module
    @GetMapping("/module/{moduleId}")
    public ResponseEntity<?> getClassesByModule(@PathVariable UUID moduleId) {
        try {
            Optional<ModuleEntity> moduleOpt = moduleRepository.findById(moduleId);
            if (moduleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Module not found");
            }

            List<ClassEntity> classes = classRepository.findByModuleAndIsActiveTrueAndIsArchivedFalseOrderByStartDateDesc(moduleOpt.get());
            List<ClassResponse> responses = classes.stream()
                    .map(this::convertClassToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching classes: " + e.getMessage());
        }
    }


    // Get class details
    @GetMapping("/{classId}")
    public ResponseEntity<?> getClassDetails(@PathVariable UUID classId) {
        try {
            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            return ResponseEntity.ok(convertClassToResponse(classOpt.get()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching class: " + e.getMessage());
        }
    }

    // Enroll student in class
    @PostMapping("/{classId}/enroll")
    public ResponseEntity<?> enrollStudent(@PathVariable UUID classId, Authentication auth) {
        try {
            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity student = userOpt.get();
            if (!"STUDENT".equals(student.getRole())) {
                return ResponseEntity.status(403).body("Only students can enroll in classes");
            }

            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            ClassEntity classEntity = classOpt.get();
            if (!classEntity.getIsActive() || classEntity.getIsArchived()) {
                return ResponseEntity.badRequest().body("Class is not available for enrollment");
            }

            // Check if student is already enrolled
            Optional<ClassEnrollmentEntity> existingEnrollment = classEnrollmentRepository
                    .findByStudentAndClassEntity(student, classEntity);
            if (existingEnrollment.isPresent()) {
                return ResponseEntity.badRequest().body("Student is already enrolled in this class");
            }

            // Check if student is enrolled in any other class for this module
            List<ClassEnrollmentEntity> existingEnrollments = classEnrollmentRepository
                    .findActiveEnrollmentsByStudentAndModule(student, classEntity.getModule());
            if (!existingEnrollments.isEmpty()) {
                return ResponseEntity.badRequest().body("Student is already enrolled in another class for this module");
            }

            // Check class capacity
            if (classEntity.getMaxStudents() != null) {
                long currentEnrollments = classEnrollmentRepository.countByClassEntityAndIsActiveTrue(classEntity);
                if (currentEnrollments >= classEntity.getMaxStudents()) {
                    return ResponseEntity.badRequest().body("Class is full");
                }
            }

            ClassEnrollmentEntity enrollment = new ClassEnrollmentEntity(student, classEntity);
            classEnrollmentRepository.save(enrollment);

            return ResponseEntity.ok("Successfully enrolled in class");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error enrolling in class: " + e.getMessage());
        }
    }

    // Get student's classes
    @GetMapping("/my-classes")
    public ResponseEntity<?> getMyClasses(Authentication auth) {
        try {
            // Check if user is authenticated
            if (auth == null || auth.getPrincipal() == null) {
                return ResponseEntity.status(401).body("Authentication required");
            }

            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            System.out.println("MyClasses - User ID from auth: " + userId);
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity user = userOpt.get();
            System.out.println("MyClasses - User: " + user.getId() + ", Role: " + user.getRole());
            
            if (!"STUDENT".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only students and admins can view their classes");
            }

            List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                    .findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(user);
            
            System.out.println("MyClasses - Found " + enrollments.size() + " enrollments for user " + user.getId());
            
            List<ClassResponse> classes = enrollments.stream()
                    .map(enrollment -> convertClassToResponse(enrollment.getClassEntity()))
                    .collect(Collectors.toList());

            System.out.println("MyClasses - Returning " + classes.size() + " classes");
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            System.err.println("Error in getMyClasses: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching classes: " + e.getMessage());
        }
    }

    // Get class students (Admin only)
    @GetMapping("/{classId}/students")
    public ResponseEntity<?> getClassStudents(@PathVariable UUID classId, Authentication auth) {
        try {
            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity user = userOpt.get();
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body("Only admins can view class students");
            }

            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            List<ClassEnrollmentEntity> enrollments = classEnrollmentRepository
                    .findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(classOpt.get());

            List<ClassStudentResponse> students = enrollments.stream()
                    .map(this::convertEnrollmentToStudentResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching students: " + e.getMessage());
        }
    }

    // Update class (Admin only)
    @PutMapping("/{classId}")
    public ResponseEntity<?> updateClass(@PathVariable UUID classId, @RequestBody UpdateClassRequest request, Authentication auth) {
        try {
            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity admin = userOpt.get();
            if (!"ADMIN".equals(admin.getRole())) {
                return ResponseEntity.status(403).body("Only admins can update classes");
            }

            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            ClassEntity classEntity = classOpt.get();
            
            // Update fields
            if (request.getName() != null) {
                classEntity.setName(request.getName());
            }
            if (request.getDescription() != null) {
                classEntity.setDescription(request.getDescription());
            }
            if (request.getModuleId() != null) {
                Optional<ModuleEntity> moduleOpt = moduleRepository.findById(request.getModuleId());
                if (moduleOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Module not found");
                }
                classEntity.setModule(moduleOpt.get());
            }
            if (request.getStartDate() != null) {
                classEntity.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                classEntity.setEndDate(request.getEndDate());
            }
            if (request.getMaxStudents() != null) {
                classEntity.setMaxStudents(request.getMaxStudents());
            }
            if (request.getIsActive() != null) {
                classEntity.setIsActive(request.getIsActive());
            }
            if (request.getIsArchived() != null) {
                classEntity.setIsArchived(request.getIsArchived());
            }

            classEntity = classRepository.save(classEntity);
            return ResponseEntity.ok(convertClassToResponse(classEntity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating class: " + e.getMessage());
        }
    }

    // Delete class (Admin only)
    @DeleteMapping("/{classId}")
    public ResponseEntity<?> deleteClass(@PathVariable UUID classId, Authentication auth) {
        try {
            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity admin = userOpt.get();
            if (!"ADMIN".equals(admin.getRole())) {
                return ResponseEntity.status(403).body("Only admins can delete classes");
            }

            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            // Check if there are active enrollments
            long activeEnrollments = classEnrollmentRepository.countByClassEntityAndIsActiveTrue(classOpt.get());
            if (activeEnrollments > 0) {
                return ResponseEntity.badRequest().body("Cannot delete class with active enrollments. Archive it instead.");
            }

            classRepository.delete(classOpt.get());
            return ResponseEntity.ok("Class deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting class: " + e.getMessage());
        }
    }

    // Archive class (Admin only)
    @PostMapping("/{classId}/archive")
    public ResponseEntity<?> archiveClass(@PathVariable UUID classId, Authentication auth) {
        try {
            // Get user ID from authentication (JWT subject)
            String userId = auth.getName();
            
            // Fetch user from database
            Optional<UserEntity> userOpt = userRepository.findById(UUID.fromString(userId));
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body("User not found");
            }
            
            UserEntity admin = userOpt.get();
            if (!"ADMIN".equals(admin.getRole())) {
                return ResponseEntity.status(403).body("Only admins can archive classes");
            }

            Optional<ClassEntity> classOpt = classRepository.findById(classId);
            if (classOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Class not found");
            }

            ClassEntity classEntity = classOpt.get();
            classEntity.setIsArchived(true);
            classEntity.setIsActive(false);
            classRepository.save(classEntity);

            return ResponseEntity.ok("Class archived successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error archiving class: " + e.getMessage());
        }
    }

    // Helper methods
    private ClassResponse convertClassToResponse(ClassEntity classEntity) {
        ClassResponse response = new ClassResponse();
        response.setId(classEntity.getId());
        response.setName(classEntity.getName());
        response.setDescription(classEntity.getDescription());
        response.setModuleId(classEntity.getModule().getId());
        response.setModuleName(classEntity.getModule().getTitle());
        response.setStartDate(classEntity.getStartDate());
        response.setEndDate(classEntity.getEndDate());
        response.setMaxStudents(classEntity.getMaxStudents());
        response.setCreatedAt(classEntity.getCreatedAt());
        
        // Count current enrollments
        long currentEnrollments = classEnrollmentRepository.countByClassEntityAndIsActiveTrue(classEntity);
        response.setCurrentEnrollments((int) currentEnrollments);
        
        // Set status fields
        response.setIsActive(classEntity.getIsActive());
        response.setIsArchived(classEntity.getIsArchived());
        
        return response;
    }

    private ClassStudentResponse convertEnrollmentToStudentResponse(ClassEnrollmentEntity enrollment) {
        ClassStudentResponse response = new ClassStudentResponse();
        response.setId(enrollment.getId());
        response.setStudentId(enrollment.getStudent().getId());
        response.setStudentName(enrollment.getStudent().getName());
        response.setStudentEmail(enrollment.getStudent().getEmail());
        response.setEnrolledAt(enrollment.getEnrolledAt());
        return response;
    }
}
