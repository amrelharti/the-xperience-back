package com.thexperience.backend.repos;

import com.thexperience.backend.entities.EnrollmentEntity;
import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    Optional<EnrollmentEntity> findByUserAndCourse(UserEntity user, CourseEntity course);
}
