package com.thexperience.backend.repos;

import com.thexperience.backend.entities.ClassEnrollmentCodeEntity;
import com.thexperience.backend.entities.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEnrollmentCodeRepository extends JpaRepository<ClassEnrollmentCodeEntity, java.util.UUID> {
    List<ClassEnrollmentCodeEntity> findByClassEntity(ClassEntity classEntity);
    Optional<ClassEnrollmentCodeEntity> findFirstByClassEntityAndCodeHashAndConsumedAtIsNull(ClassEntity classEntity, String codeHash);
}


