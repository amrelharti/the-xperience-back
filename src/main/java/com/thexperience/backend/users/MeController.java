package com.thexperience.backend.users;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MeController {
    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public Object me(Authentication auth) {
        if (auth == null || auth.getName() == null) return Map.of("authenticated", false);
        
        try {
            java.util.UUID userId = java.util.UUID.fromString(auth.getName());
            UserEntity user = users.findById(userId).orElse(null);
            if (user == null) return Map.of("authenticated", false);
            
            return Map.of(
                "authenticated", true,
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "",
                "title", user.getTitle() != null ? user.getTitle() : "",
                "instagram", user.getInstagram() != null ? user.getInstagram() : "",
                "linkedin", user.getLinkedin() != null ? user.getLinkedin() : ""
            );
        } catch (Exception e) {
            return Map.of("authenticated", false);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> profileData,
            Authentication authentication) {
        try {
            System.out.println("=== Profile Update Request Received ===");
            System.out.println("Profile Data: " + profileData);
            
            if (authentication == null || authentication.getName() == null) {
                System.out.println("ERROR: Not authenticated");
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            java.util.UUID userId = java.util.UUID.fromString(authentication.getName());
            System.out.println("User ID from auth: " + userId);
            
            Optional<UserEntity> userOpt = users.findById(userId);
            
            if (userOpt.isEmpty()) {
                System.out.println("ERROR: User not found: " + userId);
                return ResponseEntity.notFound().build();
            }
            
            UserEntity user = userOpt.get();
            System.out.println("Found user: " + user.getEmail());
            
            // Update name if provided
            if (profileData.containsKey("name") && profileData.get("name") != null && !profileData.get("name").trim().isEmpty()) {
                user.setName(profileData.get("name").trim());
            }
            
            // Update email if provided and not already taken
            if (profileData.containsKey("email") && profileData.get("email") != null && !profileData.get("email").trim().isEmpty()) {
                String newEmail = profileData.get("email").trim();
                if (!newEmail.equals(user.getEmail())) {
                    // Check if email is already taken by another user
                    Optional<UserEntity> existingUser = users.findByEmail(newEmail);
                    if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Email already taken"));
                    }
                    user.setEmail(newEmail);
                }
            }
            
            // Update image URL if provided
            if (profileData.containsKey("imageUrl")) {
                user.setImageUrl(profileData.get("imageUrl"));
            }
            
            // Update title if provided
            if (profileData.containsKey("title")) {
                user.setTitle(profileData.get("title"));
            }
            
            // Update instagram if provided
            if (profileData.containsKey("instagram")) {
                user.setInstagram(profileData.get("instagram"));
            }
            
            // Update linkedin if provided
            if (profileData.containsKey("linkedin")) {
                user.setLinkedin(profileData.get("linkedin"));
            }
            
            user.setUpdatedAt(Instant.now());
            users.save(user);
            
            System.out.println("Profile updated successfully for user: " + user.getEmail());
            System.out.println("New values - Name: " + user.getName() + ", Title: " + user.getTitle());
            
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "",
                "title", user.getTitle() != null ? user.getTitle() : "",
                "instagram", user.getInstagram() != null ? user.getInstagram() : "",
                "linkedin", user.getLinkedin() != null ? user.getLinkedin() : ""
            ));
        } catch (Exception e) {
            System.out.println("ERROR updating profile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update profile: " + e.getMessage()));
        }
    }
}
