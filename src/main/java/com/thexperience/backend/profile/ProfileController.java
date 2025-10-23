package com.thexperience.backend.profile;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test")
    public ResponseEntity<?> testProfile() {
        return ResponseEntity.ok(Map.of("message", "Profile controller is working"));
    }

    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Optional<UserEntity> userOpt = userRepository.findByEmail(userEmail);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserEntity user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "imageUrl", user.getImageUrl() != null ? user.getImageUrl() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get profile"));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> profileData,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Optional<UserEntity> userOpt = userRepository.findByEmail(userEmail);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserEntity user = userOpt.get();
            
            // Update name if provided
            if (profileData.containsKey("name") && profileData.get("name") != null && !profileData.get("name").trim().isEmpty()) {
                user.setName(profileData.get("name").trim());
            }
            
            // Update email if provided and not already taken
            if (profileData.containsKey("email") && profileData.get("email") != null && !profileData.get("email").trim().isEmpty()) {
                String newEmail = profileData.get("email").trim();
                if (!newEmail.equals(user.getEmail())) {
                    // Check if email is already taken by another user
                    Optional<UserEntity> existingUser = userRepository.findByEmail(newEmail);
                    if (existingUser.isPresent()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Email already taken"));
                    }
                    user.setEmail(newEmail);
                }
            }
            
            // Update image URL if provided
            if (profileData.containsKey("imageUrl")) {
                user.setImageUrl(profileData.get("imageUrl"));
            }
            
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "imageUrl", user.getImageUrl() != null ? user.getImageUrl() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update profile"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> passwordData,
            Authentication authentication) {
        // Password change not supported for Google OAuth users
        return ResponseEntity.badRequest().body(Map.of("error", "Password change not supported for Google OAuth users"));
    }
}