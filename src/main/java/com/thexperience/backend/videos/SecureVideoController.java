package com.thexperience.backend.videos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Secure Video Controller for Vimeo private videos
 * Provides authenticated access to Vimeo videos with domain restrictions
 */
@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class SecureVideoController {

    @Autowired
    private SecureVimeoService vimeoService;

    /**
     * Get secure Vimeo embed URL for authenticated users
     * @param videoId Vimeo video ID
     * @param auth JWT authentication
     * @return Secure embed URL or error
     */
    @GetMapping("/{videoId}/embed")
    public ResponseEntity<?> getSecureVideoUrl(
            @PathVariable String videoId, 
            Authentication auth) {
        
        try {
            // Verify user is authenticated
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Authentication required",
                    "message", "Please log in to access video content"
                ));
            }

            // Get user ID from JWT
            UUID userId = UUID.fromString(auth.getName());
            
            // Get secure Vimeo embed URL
            String embedUrl = vimeoService.getSecureEmbedUrl(videoId, userId);
            
            if (embedUrl == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Video not found",
                    "message", "This video is not available or you don't have access"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "videoId", videoId,
                "embedUrl", embedUrl,
                "secure", true,
                "domain", "localhost:3000" // Your domain restriction
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", "Invalid video ID",
                "message", "The provided video ID is not valid"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Failed to retrieve video. Please try again."
            ));
        }
    }

    /**
     * Get video metadata (title, duration, etc.) for authenticated users
     * @param videoId Vimeo video ID
     * @param auth JWT authentication
     * @return Video metadata or error
     */
    @GetMapping("/{videoId}/metadata")
    public ResponseEntity<?> getVideoMetadata(
            @PathVariable String videoId,
            Authentication auth) {
        
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Authentication required"
                ));
            }

            UUID userId = UUID.fromString(auth.getName());
            Map<String, Object> metadata = vimeoService.getVideoMetadata(videoId, userId);
            
            if (metadata == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Video not found"
                ));
            }

            return ResponseEntity.ok(metadata);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to retrieve video metadata"
            ));
        }
    }
}
