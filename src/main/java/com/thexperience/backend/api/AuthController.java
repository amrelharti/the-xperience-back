package com.thexperience.backend.api;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.security.JwtService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
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
        Map<String, Object> tokenInfo = verifyWithGoogle(body.idToken());
        if (tokenInfo == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_id_token"));
        }

        // Validate audience
        String aud = String.valueOf(tokenInfo.get("aud"));
        if (!googleClientId.equals(aud)) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_audience"));
        }

        // Extract fields
        String email   = String.valueOf(tokenInfo.get("email"));
        String name    = String.valueOf(tokenInfo.getOrDefault("name", ""));
        String picture = String.valueOf(tokenInfo.getOrDefault("picture", ""));

        // Upsert user
        UserEntity user = users.findByEmail(email).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setEmail(email);
            u.setName(name);
            u.setImageUrl(picture);
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

        Map<String, Object> bodyOut = Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "imageUrl", user.getImageUrl()
        );

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(bodyOut);
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> email(@RequestBody EmailAuthRequest body) {
        // For now, we'll use a simple hardcoded admin user
        // In production, you'd hash passwords and store them in the database
        if ("admin@thexperience.com".equals(body.email()) && "admin123".equals(body.password())) {
            // Find or create the admin user
            UserEntity user = users.findByEmail(body.email()).orElseGet(() -> {
                UserEntity u = new UserEntity();
                u.setEmail(body.email());
                u.setName("Admin User");
                u.setImageUrl("https://via.placeholder.com/150/4F46E5/FFFFFF?text=A");
                u.setRole("ADMIN");
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

            Map<String, Object> bodyOut = Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "imageUrl", user.getImageUrl()
            );

            return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(bodyOut);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cleared = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(false)          // match whatever you use for login
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header("Set-Cookie", cleared.toString()).build();
    }

    // Return a concrete type to avoid wildcard capture issues.
    private Map<String, Object> verifyWithGoogle(String idToken) {
        try {
            RestTemplate rt = new RestTemplate();
            URI uri = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rt.getForObject(uri, Map.class);
            return resp;
        } catch (Exception e) {
            return null;
        }
    }
}
