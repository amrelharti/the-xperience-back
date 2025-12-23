package com.thexperience.backend.api;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final JwtService jwtService;
    private final String googleClientId;

    public AuthController(UserRepository users,
                          JwtService jwtService,
                          @Value("${app.google-client-id}") String googleClientId) {
        this.users = users;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    public record GoogleAuthRequest(@NotBlank String idToken) {}
    public record EmailAuthRequest(@NotBlank String email, @NotBlank String password) {}

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> google(@RequestBody GoogleAuthRequest body) {
        System.out.println("Google auth request received");
        System.out.println("Expected client ID: " + googleClientId);

        Map<String, Object> tokenInfo = verifyWithGoogle(body.idToken());
        if (tokenInfo == null) {
            System.out.println("Token verification failed");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_id_token");
            return ResponseEntity.status(401).body(errorResponse);
        }

        System.out.println("Token info: " + tokenInfo);

        // Validate audience
        String aud = String.valueOf(tokenInfo.get("aud"));
        System.out.println("Token audience: '" + aud + "'");
        System.out.println("Expected audience: '" + googleClientId + "'");
        System.out.println("Token audience length: " + aud.length());
        System.out.println("Expected audience length: " + googleClientId.length());
        System.out.println("Audience match: " + googleClientId.equals(aud));
        System.out.println("Audience trim match: " + googleClientId.trim().equals(aud.trim()));

        // Use trim() to handle any whitespace issues
        if (!googleClientId.trim().equals(aud.trim())) {
            System.out.println("Audience validation failed");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_audience");
            return ResponseEntity.status(401).body(errorResponse);
        }

        // Extract fields
        String email = String.valueOf(tokenInfo.get("email")).toLowerCase().trim();
        String name = String.valueOf(tokenInfo.getOrDefault("name", ""));
        String picture = String.valueOf(tokenInfo.getOrDefault("picture", ""));

        // Normalize Gmail addresses (Gmail treats dots in local part as equivalent)
        String normalizedEmail = normalizeGmailEmail(email);
        System.out.println("Original email: " + email);
        System.out.println("Normalized email: " + normalizedEmail);

        // Check if user exists in database - try normalized email first, then original
        UserEntity user = users.findByEmail(normalizedEmail).orElse(null);
        if (user == null && !normalizedEmail.equals(email)) {
            // Try with original email if normalized is different
            user = users.findByEmail(email).orElse(null);
        }
        
        if (user == null) {
            System.out.println("User not found in database. Tried: " + normalizedEmail + " and " + email);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "user_not_found");
            errorResponse.put("message", "Your account is not registered. Please contact an administrator.");
            return ResponseEntity.status(401).body(errorResponse);
        }

        System.out.println("User found in database: " + user.getEmail() + " with role: " + user.getRole());
        
        // Debug: Check which fields are null
        System.out.println("User ID: " + user.getId());
        System.out.println("User name: " + (user.getName() != null ? user.getName() : "NULL"));
        System.out.println("User email: " + user.getEmail());
        System.out.println("User role: " + user.getRole());
        System.out.println("User imageUrl: " + (user.getImageUrl() != null ? user.getImageUrl() : "NULL"));

        // Issue JWT and set cookie for 7 days
        long ttlSeconds = Duration.ofDays(7).toSeconds();
        String jwt = jwtService.issueToken(user.getId(), user.getRole(), ttlSeconds);

        ResponseCookie cookie = ResponseCookie.from("SESSION", jwt)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")  // crucial for cross-domain cookies
                .path("/")
                .domain(".the-xperience.com") // ✅ Add dot prefix
                .maxAge(Duration.ofDays(7))
                .build();

        Map<String, Object> bodyOut = new HashMap<>();
        bodyOut.put("id", user.getId());
        bodyOut.put("name", user.getName() != null ? user.getName() : "");
        bodyOut.put("email", user.getEmail());
        bodyOut.put("role", user.getRole() != null ? user.getRole() : "");
        bodyOut.put("imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(bodyOut);
    }

        @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> email(@RequestBody EmailAuthRequest body) {
        // For now, we'll use a simple hardcoded admin user
        // In production, you'd hash passwords and store them in the database
        String normalizedEmail = body.email().toLowerCase().trim();
        
        if ("admin@thexperience.com".equals(normalizedEmail) && "admin123".equals(body.password())) {
            // Find or create the admin user
            UserEntity user = users.findByEmail(normalizedEmail).orElseGet(() -> {
                UserEntity u = new UserEntity();
                u.setEmail(normalizedEmail);
                u.setName("Admin User");
                u.setImageUrl("https://ui-avatars.com/api/?name=Admin+User&background=4F46E5&color=FFFFFF&size=150");
                u.setRole("ADMIN");
                return users.save(u);
            });

            // Issue JWT and set cookie for 7 days
            long ttlSeconds = Duration.ofDays(1).toSeconds();
            String jwt = jwtService.issueToken(user.getId(), user.getRole(), ttlSeconds);

            ResponseCookie cookie = ResponseCookie.from("SESSION", jwt)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .domain(".the-xperience.com") // ✅ Add dot prefix
                    .path("/")
                    .maxAge(ttlSeconds)
                    .build();


            Map<String, Object> bodyOut = new HashMap<>();
            bodyOut.put("id", user.getId());
            bodyOut.put("name", user.getName() != null ? user.getName() : "");
            bodyOut.put("email", user.getEmail());
            bodyOut.put("role", user.getRole() != null ? user.getRole() : "");
            bodyOut.put("imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "");

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(bodyOut);
        } else if ("test@student.com".equals(normalizedEmail) && "student123".equals(body.password())) {
            // Test student user
            UserEntity user = users.findByEmail(normalizedEmail).orElseGet(() -> {
                UserEntity u = new UserEntity();
                u.setEmail(normalizedEmail);
                u.setName("Test Student");
                u.setImageUrl("https://ui-avatars.com/api/?name=Test+Student&background=10B981&color=FFFFFF&size=150");
                u.setRole("STUDENT");
                return users.save(u);
            });

            // Issue JWT and set cookie for 7 days
            long ttlSeconds = Duration.ofDays(7).toSeconds();
            String jwt = jwtService.issueToken(user.getId(), user.getRole(), ttlSeconds);

            ResponseCookie cookie = ResponseCookie.from("SESSION", jwt)
                    .httpOnly(true)
                    .secure(false)         // set to true in production behind HTTPS
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(ttlSeconds)
                    .build();

            Map<String, Object> bodyOut = new HashMap<>();
            bodyOut.put("id", user.getId());
            bodyOut.put("name", user.getName() != null ? user.getName() : "");
            bodyOut.put("email", user.getEmail());
            bodyOut.put("role", user.getRole() != null ? user.getRole() : "");
            bodyOut.put("imageUrl", user.getImageUrl() != null ? user.getImageUrl() : "");

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(bodyOut);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "invalid_credentials");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cleared = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .domain(".the-xperience.com") // ✅ Already has dot prefix
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header("Set-Cookie", cleared.toString())
                .build();
    }

    // Return a concrete type to avoid wildcard capture issues.
    private Map<String, Object> verifyWithGoogle(String idToken) {
        try {
            System.out.println("Verifying token with Google...");
            RestTemplate rt = new RestTemplate();
            URI uri = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.getForObject(uri, Map.class);
            System.out.println("Google verification response: " + resp);
            return resp;
        } catch (Exception e) {
            System.out.println("Google verification error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Normalizes Gmail addresses by removing dots from the local part.
     * Gmail treats dots as equivalent, so youssef.aajiba@gmail.com = youssefaajiba@gmail.com
     */
    private String normalizeGmailEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domain = parts[1].toLowerCase();
        
        // Only normalize Gmail addresses
        if ("gmail.com".equals(domain) || "googlemail.com".equals(domain)) {
            // Remove dots from local part for Gmail
            String normalizedLocal = localPart.replace(".", "");
            return normalizedLocal + "@" + domain;
        }
        
        // Return original email for non-Gmail addresses
        return email;
    }
}
