package com.thexperience.backend.repos;

import com.thexperience.backend.entities.RouletteWinnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RouletteWinnerRepository extends JpaRepository<RouletteWinnerEntity, UUID> {
    
    @Query("SELECT COUNT(w) FROM RouletteWinnerEntity w WHERE w.winDate = :date")
    Long countWinnersByDate(@Param("date") LocalDate date);
    
    @Query("SELECT w FROM RouletteWinnerEntity w WHERE w.winDate = :date ORDER BY w.createdAt DESC")
    List<RouletteWinnerEntity> findWinnersByDate(@Param("date") LocalDate date);
    
    @Query("SELECT COUNT(w) FROM RouletteWinnerEntity w WHERE w.winDate = :date AND w.userIdentifier = :userIdentifier")
    Long countWinnersByDateAndUser(@Param("date") LocalDate date, @Param("userIdentifier") String userIdentifier);
    
    boolean existsByVerificationCode(String verificationCode);
    
    java.util.Optional<RouletteWinnerEntity> findByVerificationCode(String verificationCode);
    
    @Query("SELECT w FROM RouletteWinnerEntity w ORDER BY w.createdAt DESC")
    List<RouletteWinnerEntity> findAllOrderByCreatedAtDesc();
}

