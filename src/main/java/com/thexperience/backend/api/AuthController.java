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
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final JwtService jwtService;
    private final String googleClientId;

    public AuthController(UserRepository users, JwtService jwtService, @Value("${app.google-client-id}") String googleClientId) {
        this.users = users;
        this.jwtService = jwtService;
        this.googleClientId = googleClientId;
    }

    public record GoogleAuthRequest(@NotBlank String idToken) {}

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody GoogleAuthRequest body) {
        Map<?,?> tokenInfo = verifyWithGoogle(body.idToken());
        if (tokenInfo == null) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_id_token"));
        }
        String aud = String.valueOf(tokenInfo.get("aud"));
        if (!googleClientId.equals(aud)) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_audience"));
        }
        String email = String.valueOf(tokenInfo.get("email"));
        String name = String.valueOf(tokenInfo.getOrDefault("name", ""));
        String picture = String.valueOf(tokenInfo.getOrDefault("picture", ""));

        UserEntity user = users.findByEmail(email).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setEmail(email);
            u.setName(name);
            u.setImageUrl(picture);
            u.setRole("STUDENT");
            return users.save(u);
        });

        String jwt = jwtService.issueToken(user.getId(), user.getRole(), Duration.ofDays(7).toSeconds());
        ResponseCookie cookie = ResponseCookie.from("SESSION", jwt)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail(), "role", user.getRole(), "imageUrl", user.getImageUrl()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cleared = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header("Set-Cookie", cleared.toString()).build();
    }

    private Map<?,?> verifyWithGoogle(String idToken) {
        try {
            RestTemplate rt = new RestTemplate();
            URI uri = URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken);
            Map<?,?> resp = rt.getForObject(uri, Map.class);
            return resp;
        } catch (Exception e) {
            return null;
        }
    }
}
