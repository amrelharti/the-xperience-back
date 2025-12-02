package com.thexperience.backend.repos;

import com.thexperience.backend.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    long countByRole(String role);
    long countByCreatedAtAfter(Instant date);
    List<UserEntity> findByCreatedAtAfter(Instant date);
    List<UserEntity> findByRole(String role);
}
