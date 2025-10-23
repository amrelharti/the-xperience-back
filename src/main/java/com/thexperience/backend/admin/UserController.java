package com.thexperience.backend.admin;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
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
    public Map<String, String> deleteUser(@PathVariable UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        
        userRepository.deleteById(id);
        return Map.of("message", "User deleted successfully");
    }

    @PostMapping
    public UserEntity createUser(@RequestBody CreateUserRequest request) {
        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setEmail(request.email());
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
