package com.thexperience.backend.admin;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

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
        return saveNewUser(request.name(), request.email(), request.role(), request.imageUrl());
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkCreateUsersResponse> bulkCreateUsers(@RequestBody BulkCreateUsersRequest request) {
        String duplicateStrategy = request.duplicateStrategy() != null
                ? request.duplicateStrategy().toUpperCase()
                : "SKIP";

        if (!duplicateStrategy.equals("SKIP") && !duplicateStrategy.equals("FAIL") && !duplicateStrategy.equals("UPDATE")) {
            return ResponseEntity.badRequest().body(new BulkCreateUsersResponse(
                    List.of(),
                    List.of(),
                    List.of(new BulkUserResult(0, null, null, "Invalid duplicateStrategy. Use SKIP, FAIL, or UPDATE.")),
                    List.of()
            ));
        }

        List<UserEntity> created = new ArrayList<>();
        List<BulkUserResult> skipped = new ArrayList<>();
        List<BulkUserResult> failed = new ArrayList<>();
        List<UserEntity> updated = new ArrayList<>();

        List<BulkUserRow> rows = request.users() != null ? request.users() : List.of();
        java.util.Set<String> seenEmails = new java.util.HashSet<>();

        for (int i = 0; i < rows.size(); i++) {
            BulkUserRow row = rows.get(i);
            int rowNumber = row.rowNumber() != null ? row.rowNumber() : i + 2;

            String name = row.name() != null ? row.name().trim() : "";
            String email = row.email() != null ? row.email().toLowerCase().trim() : "";

            if (name.isEmpty() || email.isEmpty()) {
                failed.add(new BulkUserResult(rowNumber, name, email, "Name and email are required"));
                continue;
            }

            if (!EMAIL_PATTERN.matcher(email).matches()) {
                failed.add(new BulkUserResult(rowNumber, name, email, "Invalid email format"));
                continue;
            }

            if (!seenEmails.add(email)) {
                failed.add(new BulkUserResult(rowNumber, name, email, "Duplicate email in file"));
                continue;
            }

            Optional<UserEntity> existing = userRepository.findByEmail(email);
            if (existing.isPresent()) {
                switch (duplicateStrategy) {
                    case "SKIP" -> skipped.add(new BulkUserResult(rowNumber, name, email, "User already exists"));
                    case "FAIL" -> failed.add(new BulkUserResult(rowNumber, name, email, "User already exists"));
                    case "UPDATE" -> {
                        UserEntity user = existing.get();
                        user.setName(name);
                        user.setUpdatedAt(java.time.Instant.now());
                        updated.add(userRepository.save(user));
                    }
                }
                continue;
            }

            try {
                String role = request.role() != null && !request.role().isBlank() ? request.role() : "STUDENT";
                created.add(saveNewUser(name, email, role, null));
            } catch (Exception e) {
                failed.add(new BulkUserResult(rowNumber, name, email, "Failed to create user: " + e.getMessage()));
            }
        }

        return ResponseEntity.ok(new BulkCreateUsersResponse(created, skipped, failed, updated));
    }

    private UserEntity saveNewUser(String name, String email, String role, String imageUrl) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(email != null ? email.toLowerCase().trim() : null);
        user.setRole(role != null && !role.isBlank() ? role : "STUDENT");
        user.setImageUrl(imageUrl);
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

    public record BulkUserRow(
        Integer rowNumber,
        String name,
        String email
    ) {}

    public record BulkCreateUsersRequest(
        List<BulkUserRow> users,
        String duplicateStrategy,
        String role
    ) {}

    public record BulkUserResult(
        int rowNumber,
        String name,
        String email,
        String reason
    ) {}

    public record BulkCreateUsersResponse(
        List<UserEntity> created,
        List<BulkUserResult> skipped,
        List<BulkUserResult> failed,
        List<UserEntity> updated
    ) {}
}
