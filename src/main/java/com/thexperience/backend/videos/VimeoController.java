package com.thexperience.backend.videos;

import com.thexperience.backend.entities.UserEntity;
import com.thexperience.backend.repos.UserRepository;
import com.thexperience.backend.services.VimeoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VimeoController {
    
    private final VimeoService vimeoService;
    private final UserRepository userRepository;
    
    public VimeoController(VimeoService vimeoService, UserRepository userRepository) {
        this.vimeoService = vimeoService;
        this.userRepository = userRepository;
    }
    
    /**
     * Get signed Vimeo embed URL for a video
     */
    @GetMapping("/vimeo/{videoId}/signed-url")
    public ResponseEntity<?> getSignedVimeoUrl(
            @PathVariable String videoId,
            Authentication auth,
            HttpServletRequest request) {
        
        try {
            // Get user from authentication
            String userId = auth.getName();
            UserEntity user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get user IP
            String userIp = getClientIpAddress(request);
            
            // Generate signed URL
            String signedUrl = vimeoService.generateSignedEmbedUrl(videoId, user, userIp);
            
            return ResponseEntity.ok(Map.of(
                "signedUrl", signedUrl,
                "videoId", videoId,
                "expiresIn", 1800 // 30 minutes in seconds
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to generate signed URL: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Accept terms of service for video access
     */
    @PostMapping("/vimeo/{videoId}/accept-terms")
    public ResponseEntity<?> acceptTerms(
            @PathVariable String videoId,
            Authentication auth) {
        
        try {
            String userId = auth.getName();
            UserEntity user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Here you would typically store the terms acceptance
            // For now, just return success
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Terms of service accepted",
                "videoId", videoId
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to accept terms: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get video analytics
     */
    @GetMapping("/vimeo/{videoId}/analytics")
    public ResponseEntity<?> getVideoAnalytics(
            @PathVariable String videoId,
            Authentication auth) {
        
        try {
            // Check if user is admin
            String userId = auth.getName();
            UserEntity user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!"ADMIN".equals(user.getRole())) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Access denied. Admin role required."
                ));
            }
            
            Map<String, Object> analytics = vimeoService.getVideoAnalytics(videoId);
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get analytics: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

