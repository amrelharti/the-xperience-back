package com.thexperience.backend.sessions;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class LiveSessionController {

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private LiveSessionAttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.thexperience.backend.repos.TrainingModuleRepository trainingModuleRepository;
    
    @Autowired(required = false)
    private com.thexperience.backend.services.ProgressService progressService;

    @Autowired
    private ClassRepository classRepository;

    // Create live session (PROFESSOR/ADMIN only) - DEPRECATED: Use /api/admin/modules/{moduleId}/live-session instead
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            // Check if user has permission to create sessions
            if (!"PROFESSOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only professors and admins can create sessions"));
            }

            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(request.getModuleId()).orElseThrow();
            
            // classId is required - get from request or find first class for this training
            UUID classId = request.getClassId();
            ClassEntity classEntity;
            if (classId == null) {
                // Try to find first active class for this training
                List<ClassEntity> classes = classRepository.findByModuleAndIsActiveTrueAndIsArchivedFalseOrderByStartDateDesc(trainingModule.getTraining());
                if (classes.isEmpty()) {
                    throw new IllegalArgumentException("No active class found for this training. classId is required.");
                }
                classEntity = classes.get(0);
            } else {
                classEntity = classRepository.findById(classId).orElseThrow();
                // Validate that class belongs to the training
                if (!classEntity.getModule().getId().equals(trainingModule.getTraining().getId())) {
                    throw new IllegalArgumentException("Class does not belong to the training of this module.");
                }
            }
            
            LiveSessionEntity session = new LiveSessionEntity(
                trainingModule,
                classEntity,
                request.getTitle(), 
                request.getDescription(),
                request.getStartsAt(), 
                request.getEndsAt(), 
                request.getLocation()
            );
            
            session = liveSessionRepository.save(session);
            
            return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "title", session.getTitle(),
                "description", session.getDescription(),
                "startsAt", session.getStartsAt(),
                "endsAt", session.getEndsAt(),
                "location", session.getLocation(),
                "trainingModule", Map.of(
                    "id", trainingModule.getId(),
                    "title", trainingModule.getTitle()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create session: " + e.getMessage()));
        }
    }

    // Get sessions by training module
    @GetMapping("/training-modules/{moduleId}/sessions")
    public ResponseEntity<?> getTrainingModuleSessions(@PathVariable UUID moduleId, Authentication auth) {
        try {
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            List<LiveSessionEntity> sessions = liveSessionRepository.findByTrainingModuleOrderByStartsAtAsc(trainingModule);
            
            List<Map<String, Object>> sessionList = sessions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("sessions", sessionList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch sessions: " + e.getMessage()));
        }
    }

    // Get upcoming sessions
    @GetMapping("/sessions/upcoming")
    public ResponseEntity<?> getUpcomingSessions(Authentication auth) {
        try {
            List<LiveSessionEntity> sessions = liveSessionRepository.findUpcomingSessions(Instant.now());
            
            List<Map<String, Object>> sessionList = sessions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("sessions", sessionList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch upcoming sessions: " + e.getMessage()));
        }
    }

    // Mark attendance for a session (ADMIN/PROFESSOR only - for manual attendance)
    @PostMapping("/sessions/{sessionId}/attend")
    public ResponseEntity<?> markAttendance(@PathVariable UUID sessionId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            LiveSessionEntity session = liveSessionRepository.findById(sessionId).orElseThrow();
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();

            // Check if user is already marked as attended in this class
            if (attendanceRepository.existsBySessionAndUserAndClassEntity(session, user, classEntity)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Already marked as attended"));
            }

            LiveSessionAttendanceEntity attendance = new LiveSessionAttendanceEntity(session, user, classEntity);
            attendanceRepository.save(attendance);

            // Update training module progress
            updateModuleProgress(user, session.getTrainingModule(), classEntity);

            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "attendedAt", attendance.getAttendedAt(),
                "message", "Attendance marked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to mark attendance: " + e.getMessage()));
        }
    }

    // Get session attendance (PROFESSOR/ADMIN only)
    @GetMapping("/sessions/{sessionId}/attendance")
    public ResponseEntity<?> getSessionAttendance(@PathVariable UUID sessionId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            // Check if user has permission to view attendance
            if (!"PROFESSOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only professors and admins can view attendance"));
            }

            LiveSessionEntity session = liveSessionRepository.findById(sessionId).orElseThrow();
            // Note: This method needs to be updated to handle class context properly
            // For now, we'll get all attendance records for the session
            List<LiveSessionAttendanceEntity> attendance = attendanceRepository.findAll().stream()
                .filter(att -> att.getSession().getId().equals(sessionId))
                .sorted((a, b) -> a.getAttendedAt().compareTo(b.getAttendedAt()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> attendanceList = attendance.stream()
                .map(att -> Map.of(
                    "user", Map.of(
                        "id", att.getUser().getId(),
                        "name", att.getUser().getName(),
                        "email", att.getUser().getEmail()
                    ),
                    "attendedAt", att.getAttendedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "session", Map.of(
                    "id", session.getId(),
                    "title", session.getTitle(),
                    "startsAt", session.getStartsAt(),
                    "endsAt", session.getEndsAt()
                ),
                "attendance", attendanceList,
                "totalAttended", attendance.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch attendance: " + e.getMessage()));
        }
    }

    // Helper method to convert entity to response
    private Map<String, Object> convertToResponse(LiveSessionEntity session) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", session.getId());
        response.put("title", session.getTitle());
        response.put("description", session.getDescription() != null ? session.getDescription() : "");
        response.put("startsAt", session.getStartsAt());
        response.put("endsAt", session.getEndsAt());
        response.put("location", session.getLocation() != null ? session.getLocation() : "");
        response.put("meetingUrl", session.getMeetingUrl() != null ? session.getMeetingUrl() : "");
        response.put("meetingPassword", session.getMeetingPassword() != null ? session.getMeetingPassword() : "");
        response.put("durationMinutes", session.getDurationMinutes() != null ? session.getDurationMinutes() : 60);
        response.put("instructorName", session.getInstructorName() != null ? session.getInstructorName() : "");
        response.put("isArchived", session.getIsArchived() != null ? session.getIsArchived() : false);
        response.put("trainingModule", Map.of(
            "id", session.getTrainingModule().getId(),
            "title", session.getTrainingModule().getTitle()
        ));
        if (session.getClassEntity() != null) {
            response.put("class", Map.of(
                "id", session.getClassEntity().getId(),
                "name", session.getClassEntity().getName()
            ));
        }
        return response;
    }

    // Get module progress (simple percentage)
    @GetMapping("/modules/{moduleId}/training-progress")
    public ResponseEntity<?> getModuleProgress(@PathVariable UUID moduleId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            
            if (progressService != null) {
                int progress = progressService.calculateModuleProgress(userId, moduleId, classId);
                return ResponseEntity.ok(Map.of("progress", progress));
            } else {
                return ResponseEntity.ok(Map.of("progress", 0));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get progress: " + e.getMessage()));
        }
    }

    // Get module progress details
    @GetMapping("/modules/{moduleId}/progress-details")
    public ResponseEntity<?> getModuleProgressDetails(@PathVariable UUID moduleId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            
            if (progressService != null) {
                Map<String, Object> progress = progressService.getModuleProgressDetails(userId, moduleId, classId);
                return ResponseEntity.ok(progress);
            } else {
                return ResponseEntity.ok(Map.of(
                    "totalProgress", 0,
                    "message", "Progress service not available"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get progress: " + e.getMessage()));
        }
    }
    
    // Helper method to update training module progress
    private void updateModuleProgress(UserEntity user, TrainingModuleEntity trainingModule, ClassEntity classEntity) {
        // Progress is calculated dynamically via ProgressService
        // No need to update a separate progress record
        if (progressService != null) {
            int progress = progressService.calculateModuleProgress(user.getId(), trainingModule.getId(), classEntity.getId());
            System.out.println("Training module progress updated: " + progress + "%");
        }
    }

    // Check if user attended a session (requires classId for class-specific attendance)
    @GetMapping("/sessions/{sessionId}/my-attendance")
    public ResponseEntity<?> getMyAttendance(@PathVariable UUID sessionId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            LiveSessionEntity session = liveSessionRepository.findById(sessionId).orElseThrow();
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();

            boolean attended = attendanceRepository.existsBySessionAndUserAndClassEntity(session, user, classEntity);
            
            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "attended", attended
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to check attendance: " + e.getMessage()));
        }
    }

    // Request DTOs
    public static class CreateSessionRequest {
        private UUID moduleId;
        private UUID classId;
        private String title;
        private String description;
        private Instant startsAt;
        private Instant endsAt;
        private String location;
        private String meetingUrl;
        private String meetingPassword;
        private Integer durationMinutes;
        private String instructorName;

        // Getters and setters
        public UUID getModuleId() { return moduleId; }
        public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }
        public UUID getClassId() { return classId; }
        public void setClassId(UUID classId) { this.classId = classId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Instant getStartsAt() { return startsAt; }
        public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
        public Instant getEndsAt() { return endsAt; }
        public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getMeetingUrl() { return meetingUrl; }
        public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }
        public String getMeetingPassword() { return meetingPassword; }
        public void setMeetingPassword(String meetingPassword) { this.meetingPassword = meetingPassword; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public String getInstructorName() { return instructorName; }
        public void setInstructorName(String instructorName) { this.instructorName = instructorName; }
    }

    // Admin: Create or update live session for a training module and class
    @PostMapping("/admin/modules/{moduleId}/live-session")
    public ResponseEntity<?> createOrUpdateLiveSession(
            @PathVariable UUID moduleId, 
            @RequestParam(required = false) UUID classId,
            @RequestBody CreateSessionRequest request, 
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            
            // classId can come from query parameter or request body
            UUID finalClassId = classId != null ? classId : request.getClassId();
            if (finalClassId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            
            ClassEntity classEntity = classRepository.findById(finalClassId).orElseThrow();
            
            // Validate that class belongs to the same training as the module
            if (!classEntity.getModule().getId().equals(trainingModule.getTraining().getId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Class does not belong to the training of this module"));
            }
            
            // Check if live session already exists for this training module AND class
            Optional<LiveSessionEntity> existingSession = liveSessionRepository.findByTrainingModuleAndClassEntity(trainingModule, classEntity);
            
            LiveSessionEntity liveSession;
            if (existingSession.isPresent()) {
                // Update existing session
                liveSession = existingSession.get();
                liveSession.setTitle(request.getTitle());
                liveSession.setDescription(request.getDescription());
                liveSession.setStartsAt(request.getStartsAt());
                liveSession.setEndsAt(request.getEndsAt());
                liveSession.setLocation(request.getLocation());
                liveSession.setMeetingUrl(request.getMeetingUrl());
                liveSession.setMeetingPassword(request.getMeetingPassword());
                liveSession.setDurationMinutes(request.getDurationMinutes());
                liveSession.setInstructorName(request.getInstructorName());
                liveSession.setIsArchived(false); // Reset archived status if updating
                liveSession.setUpdatedAt(Instant.now());
            } else {
                // Create new session
                liveSession = new LiveSessionEntity(
                    trainingModule,
                    classEntity,
                    request.getTitle(),
                    request.getDescription(),
                    request.getStartsAt(),
                    request.getEndsAt(),
                    request.getLocation(),
                    request.getMeetingUrl(),
                    request.getMeetingPassword(),
                    request.getDurationMinutes(),
                    request.getInstructorName()
                );
            }
            
            liveSession = liveSessionRepository.save(liveSession);
            return ResponseEntity.ok(convertToResponse(liveSession));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to save live session: " + e.getMessage()));
        }
    }

    // Admin: Get live session for a training module and class
    @GetMapping("/admin/modules/{moduleId}/live-session")
    public ResponseEntity<?> getLiveSessionForModule(
            @PathVariable UUID moduleId, 
            @RequestParam(required = false) UUID classId,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            
            if (classId == null) {
                // If no classId provided, return first active session (backward compatibility)
                Optional<LiveSessionEntity> liveSession = liveSessionRepository.findByTrainingModuleAndIsArchivedFalse(trainingModule);
                if (liveSession.isPresent()) {
                    return ResponseEntity.ok(convertToResponse(liveSession.get()));
                } else {
                    return ResponseEntity.ok(Map.of("message", "No live session found for this training module"));
                }
            }
            
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
            Optional<LiveSessionEntity> liveSession = liveSessionRepository.findByTrainingModuleAndClassEntity(trainingModule, classEntity);
            
            if (liveSession.isPresent()) {
                return ResponseEntity.ok(convertToResponse(liveSession.get()));
            } else {
                return ResponseEntity.ok(Map.of("message", "No live session found for this training module and class"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get live session: " + e.getMessage()));
        }
    }

    // Admin: Delete live session for a training module and class
    @DeleteMapping("/admin/modules/{moduleId}/live-session")
    public ResponseEntity<?> deleteLiveSessionForModule(
            @PathVariable UUID moduleId, 
            @RequestParam(required = false) UUID classId,
            Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
            }
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            
            if (classId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
            Optional<LiveSessionEntity> liveSession = liveSessionRepository.findByTrainingModuleAndClassEntity(trainingModule, classEntity);
            
            if (liveSession.isPresent()) {
                liveSessionRepository.delete(liveSession.get());
                return ResponseEntity.ok(Map.of("message", "Live session deleted successfully"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "No live session found for this training module and class"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete live session: " + e.getMessage()));
        }
    }

    // Student: Get live session for a training module and class
    @GetMapping("/modules/{moduleId}/live-session")
    public ResponseEntity<?> getStudentLiveSession(
            @PathVariable UUID moduleId, 
            @RequestParam(required = false) UUID classId,
            Authentication auth) {
        try {
            System.out.println("🔍 Student live session request for module: " + moduleId + ", classId: " + classId);
            
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            System.out.println("👤 User found: " + user.getName() + " (" + user.getRole() + ")");
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            System.out.println("📚 Training module found: " + trainingModule.getTitle());
            
            if (classId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "classId is required"));
            }
            
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
            
            // Verify student is enrolled in this class
            // Note: You may want to add enrollment verification here
            
            // Get active (non-archived) live session for this module and class
            Optional<LiveSessionEntity> liveSession = liveSessionRepository.findByTrainingModuleAndClassEntityAndIsArchivedFalse(trainingModule, classEntity);
            
            if (liveSession.isPresent()) {
                System.out.println("✅ Live session found: " + liveSession.get().getTitle());
                return ResponseEntity.ok(convertToResponse(liveSession.get()));
            } else {
                System.out.println("ℹ️ No live session found for this training module and class");
                return ResponseEntity.ok(Map.of("message", "No live session found for this training module"));
            }
        } catch (Exception e) {
            System.out.println("❌ Error in getStudentLiveSession: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get live session: " + e.getMessage()));
        }
    }

    // Student: Mark attendance for live session
    @PostMapping("/modules/{moduleId}/live-session/attend")
    public ResponseEntity<?> markStudentAttendance(@PathVariable UUID moduleId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            Optional<LiveSessionEntity> liveSessionOpt = liveSessionRepository.findByTrainingModule(trainingModule);
            
            if (liveSessionOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No live session found for this training module"));
            }
            
            LiveSessionEntity liveSession = liveSessionOpt.get();
            
            // Check if the live session has started
            Instant now = Instant.now();
            if (now.isBefore(liveSession.getStartsAt())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot mark attendance before the live session starts",
                    "sessionStartsAt", liveSession.getStartsAt(),
                    "currentTime", now
                ));
            }
            
            // Check if user is already marked as attended in this class
            if (attendanceRepository.existsBySessionAndUserAndClassEntity(liveSession, user, classEntity)) {
                return ResponseEntity.ok(Map.of("attended", true, "message", "Already marked as attended"));
            }

            LiveSessionAttendanceEntity attendance = new LiveSessionAttendanceEntity(liveSession, user, classEntity);
            attendanceRepository.save(attendance);

            // Update training module progress
            updateModuleProgress(user, trainingModule, classEntity);

            return ResponseEntity.ok(Map.of(
                "attended", true,
                "attendedAt", attendance.getAttendedAt(),
                "message", "Attendance marked successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to mark attendance: " + e.getMessage()));
        }
    }

    // Student: Check attendance status for live session
    @GetMapping("/modules/{moduleId}/live-session/attend")
    public ResponseEntity<?> checkStudentAttendance(@PathVariable UUID moduleId, @RequestParam UUID classId, Authentication auth) {
        try {
            UUID userId = UUID.fromString(auth.getName());
            UserEntity user = userRepository.findById(userId).orElseThrow();
            ClassEntity classEntity = classRepository.findById(classId).orElseThrow();
            
            TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
            Optional<LiveSessionEntity> liveSessionOpt = liveSessionRepository.findByTrainingModule(trainingModule);
            
            if (liveSessionOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No live session found for this training module"));
            }
            
            LiveSessionEntity liveSession = liveSessionOpt.get();
            boolean attended = attendanceRepository.existsBySessionAndUserAndClassEntity(liveSession, user, classEntity);
            
            return ResponseEntity.ok(Map.of("attended", attended));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to check attendance: " + e.getMessage()));
        }
    }
}
