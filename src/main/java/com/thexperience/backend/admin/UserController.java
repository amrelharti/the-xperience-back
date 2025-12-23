package com.thexperience.backend.admin;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public UserEntity getUserById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PutMapping("/{id}")
    public UserEntity updateUser(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.imageUrl() != null) {
            user.setImageUrl(request.imageUrl());
        }
        
        return userRepository.save(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "User not found"));
        }
        
        try {
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                    "error", "Cannot delete user",
                    "message", "This user has associated data (consumed access codes, enrollments, etc.). Please run the latest database migration (V43) to fix foreign key constraints, or contact system administrator."
                ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete user", "details", e.getMessage()));
        }
    }

    @PostMapping
    public UserEntity createUser(@RequestBody CreateUserRequest request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        // Normalize email to lowercase to match database constraint
        user.setEmail(request.email() != null ? request.email().toLowerCase().trim() : null);
        user.setRole(request.role());
        user.setImageUrl(request.imageUrl());
        user.setCreatedAt(java.time.Instant.now());
        user.setUpdatedAt(java.time.Instant.now());
        
        return userRepository.save(user);
    }

    @GetMapping("/stats")
    public Map<String, Object> getUserStats() {
        long totalUsers = userRepository.count();
        long adminUsers = userRepository.countByRole("ADMIN");
        long studentUsers = userRepository.countByRole("STUDENT");
        
        return Map.of(
            "totalUsers", totalUsers,
            "adminUsers", adminUsers,
            "studentUsers", studentUsers
        );
    }

    public record CreateUserRequest(
        String name,
        String email,
        String role,
        String imageUrl
    ) {}

    public record UpdateUserRequest(
        String name,
        String role,
        String imageUrl
    ) {}
}
