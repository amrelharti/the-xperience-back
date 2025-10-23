package com.thexperience.backend.repos;

import com.thexperience.backend.entities.CourseAccessCodeEntity;
import com.thexperience.backend.entities.CourseEntity;
import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseAccessCodeRepository extends JpaRepository<CourseAccessCodeEntity, java.util.UUID> {
    
    List<CourseAccessCodeEntity> findByCourseOrderByCreatedAtDesc(CourseEntity course);
    
    Optional<CourseAccessCodeEntity> findByCodeHash(String codeHash);
    
    List<CourseAccessCodeEntity> findByConsumedBy(UserEntity user);
    
    List<CourseAccessCodeEntity> findByGeneratedBy(UserEntity user);
}

