package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEnrollmentEntity;
import com.thexperience.backend.entities.ClassEntity;
import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.entities.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentEntity, UUID> {
    
    // Find enrollments by student
    List<ClassEnrollmentEntity> findByStudentOrderByEnrolledAtDesc(UserEntity student);
    
    // Find active enrollments by student
    List<ClassEnrollmentEntity> findByStudentAndIsActiveTrueOrderByEnrolledAtDesc(UserEntity student);
    
    // Find enrollments by class
    List<ClassEnrollmentEntity> findByClassEntityOrderByEnrolledAtAsc(ClassEntity classEntity);
    
    // Find active enrollments by class
    List<ClassEnrollmentEntity> findByClassEntityAndIsActiveTrueOrderByEnrolledAtAsc(ClassEntity classEntity);
    
    // Find specific enrollment
    Optional<ClassEnrollmentEntity> findByStudentAndClassEntity(UserEntity student, ClassEntity classEntity);
    
    // Find active enrollment
    Optional<ClassEnrollmentEntity> findByStudentAndClassEntityAndIsActiveTrue(UserEntity student, ClassEntity classEntity);
    
    // Count active enrollments for a class
    long countByClassEntityAndIsActiveTrue(ClassEntity classEntity);
    
    // Check if student is enrolled in any class for a module
    @Query("SELECT ce FROM ClassEnrollmentEntity ce WHERE ce.student = :student AND ce.classEntity.module = :module AND ce.isActive = true")
    List<ClassEnrollmentEntity> findActiveEnrollmentsByStudentAndModule(@Param("student") UserEntity student, @Param("module") ModuleEntity module);
}
