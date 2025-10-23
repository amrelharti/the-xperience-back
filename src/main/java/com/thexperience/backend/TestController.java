package com.thexperience.backend;

import com.thexperience.backend.security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private final JwtService jwtService;
    
    public TestController(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @GetMapping("/jwt")
    public Map<String, Object> testJwt() {
        try {
            // Create a test token
            UUID testUserId = UUID.randomUUID();
            String testRole = "ADMIN";
            String token = jwtService.issueToken(testUserId, testRole, 3600); // 1 hour
            
            // Try to parse it back
            var claims = jwtService.parse(token);
            String parsedUserId = claims.getSubject();
            String parsedRole = String.valueOf(claims.get("role"));
            
            return Map.of(
                "success", true,
                "originalUserId", testUserId.toString(),
                "parsedUserId", parsedUserId,
                "originalRole", testRole,
                "parsedRole", parsedRole,
                "token", token
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}

