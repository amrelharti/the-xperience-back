package com.thexperience.backend.admin;

import com.thexperience.backend.entities.LandingPageViewEntity;
import com.thexperience.backend.repos.LandingPageViewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LandingPageViewController {

    @Autowired
    private LandingPageViewRepository viewRepository;

    /**
     * Track a landing page view
     * Public endpoint - no authentication required
     */
    @PostMapping("/api/landing-page/track-view")
    public ResponseEntity<?> trackView(@RequestBody Map<String, String> request) {
        try {
            LandingPageViewEntity view = new LandingPageViewEntity();
            
            // Set optional fields if provided
            if (request.get("ip") != null) {
                view.setIpAddress(request.get("ip"));
            }
            if (request.get("userAgent") != null) {
                view.setUserAgent(request.get("userAgent"));
            }
            if (request.get("referer") != null) {
                view.setReferer(request.get("referer"));
            }
            
            // Save to database
            LandingPageViewEntity saved = viewRepository.save(view);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "View tracked successfully");
            response.put("id", saved.getId().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("Error tracking landing page view: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to track view: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get total landing page views count
     * Admin endpoint - requires authentication
     */
    @GetMapping("/api/admin/landing-page/views")
    public ResponseEntity<?> getViewCount() {
        try {
            long totalViews = viewRepository.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalViews", totalViews);
            response.put("message", "View count retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error fetching landing page view count: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch view count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

