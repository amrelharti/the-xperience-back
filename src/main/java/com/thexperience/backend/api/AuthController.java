package com.thexperience.backend.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    public record GoogleAuthRequest(@NotBlank String idToken) {}

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody GoogleAuthRequest body) {
        // TODO: verify Google ID token, create/find user, set HttpOnly JWT cookie
        return ResponseEntity.ok(Map.of("message", "stub"));
    }
}
