package com.thexperience.backend.services;

import com.thexperience.backend.entities.*;
import com.thexperience.backend.repos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {

    @Autowired
    private ModuleRepository moduleRepository;
    
    @Autowired
    private com.thexperience.backend.repos.TrainingModuleRepository trainingModuleRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private LiveSessionAttendanceRepository attendanceRepository;

    @Autowired
    private ModuleProgressRepository moduleProgressRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Calculate training module progress for a user
     * Formula: 50% from live session attendance + 50% from course completion
     *
     * @param userId User ID
     * @param moduleId TrainingModule ID (not Training ID)
     * @return Progress percentage (0-100)
     */
    public int calculateModuleProgress(UUID userId, UUID moduleId) {
        TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
        UserEntity user = userRepository.findById(userId).orElseThrow();

        // 1. Calculate live session progress (50%)
        double liveSessionProgress = 0.0;
        List<LiveSessionEntity> liveSessions = liveSessionRepository.findByTrainingModuleOrderByStartsAtAsc(trainingModule);
        
        if (!liveSessions.isEmpty()) {
            int attendedSessions = 0;
            for (LiveSessionEntity session : liveSessions) {
                if (attendanceRepository.existsBySessionAndUser(session, user)) {
                    attendedSessions++;
                }
            }
            liveSessionProgress = ((double) attendedSessions / liveSessions.size()) * 50.0;
        } else {
            // If no live sessions exist, live session progress is 0%
            liveSessionProgress = 0.0;
        }

        // 2. Calculate course completion progress (50%)
        double courseProgress = 0.0;
        List<CourseEntity> courses = courseRepository.findByTrainingModuleOrderByOrderIndexAsc(trainingModule);
        
        if (!courses.isEmpty()) {
            long completedCourses = courseProgressRepository.countCompletedCoursesByUserAndTrainingModule(user, trainingModule);
            courseProgress = ((double) completedCourses / courses.size()) * 50.0;
        } else {
            // If no courses exist, course progress is 0%
            courseProgress = 0.0;
        }

        // 3. Total progress
        int totalProgress = (int) Math.round(liveSessionProgress + courseProgress);
        
        return Math.min(100, Math.max(0, totalProgress));
    }

    /**
     * Calculate training-wide progress for a user (across all modules in a class)
     *
     * @param userId User ID
     * @param classId Class ID
     * @return Progress percentage (0-100)
     */
    public int calculateTrainingProgress(UUID userId, UUID classId) {
        // TODO: Implement training-wide progress
        // This would calculate average progress across all modules in the class
        return 0;
    }

    /**
     * Get detailed progress breakdown for a training module
     *
     * @param userId User ID
     * @param moduleId TrainingModule ID
     * @return Progress details map
     */
    public java.util.Map<String, Object> getModuleProgressDetails(UUID userId, UUID moduleId) {
        TrainingModuleEntity trainingModule = trainingModuleRepository.findById(moduleId).orElseThrow();
        UserEntity user = userRepository.findById(userId).orElseThrow();

        java.util.Map<String, Object> details = new java.util.HashMap<>();

        // Live sessions
        List<LiveSessionEntity> liveSessions = liveSessionRepository.findByTrainingModuleOrderByStartsAtAsc(trainingModule);
        int attendedSessions = 0;
        for (LiveSessionEntity session : liveSessions) {
            if (attendanceRepository.existsBySessionAndUser(session, user)) {
                attendedSessions++;
            }
        }
        
        details.put("totalLiveSessions", liveSessions.size());
        details.put("attendedLiveSessions", attendedSessions);
        details.put("liveSessionProgress", liveSessions.isEmpty() ? 0.0 : ((double) attendedSessions / liveSessions.size()) * 50.0);

        // Courses
        List<CourseEntity> courses = courseRepository.findByTrainingModuleOrderByOrderIndexAsc(trainingModule);
        long completedCourses = courseProgressRepository.countCompletedCoursesByUserAndTrainingModule(user, trainingModule);
        double courseProgress = courses.isEmpty() ? 0.0 : ((double) completedCourses / courses.size()) * 50.0;
        
        details.put("totalCourses", courses.size());
        details.put("completedCourses", completedCourses);
        details.put("courseProgress", courseProgress);

        // Total
        details.put("totalProgress", calculateModuleProgress(userId, moduleId));

        return details;
    }
}

