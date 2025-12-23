package com.thexperience.backend.admin;

import com.thexperience.backend.entities.LandingPageSettingsEntity;
import com.thexperience.backend.repos.LandingPageSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class LandingPageSettingsController {

    @Autowired
    private LandingPageSettingsRepository settingsRepository;

    @GetMapping("/api/landing-page/remaining-spots")
    public ResponseEntity<?> getRemainingSpots() {
        try {
            Optional<LandingPageSettingsEntity> settings = settingsRepository.findFirstByOrderByUpdatedAtDesc();
            
            if (settings.isPresent()) {
                Integer spots = settings.get().getRemainingSpots();
                System.out.println("Fetching remaining spots: " + spots + " (ID: " + settings.get().getId() + ", UpdatedAt: " + settings.get().getUpdatedAt() + ")");
                Map<String, Object> response = new HashMap<>();
                response.put("remainingSpots", spots);
                return ResponseEntity.ok(response);
            } else {
                System.out.println("No settings found, returning default: 12");
                // Return default value if no settings exist
                Map<String, Object> response = new HashMap<>();
                response.put("remainingSpots", 12);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            System.err.println("Error fetching remaining spots: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch remaining spots: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/api/admin/landing-page/settings/remaining-spots")
    public ResponseEntity<?> getRemainingSpotsAdmin() {
        return getRemainingSpots();
    }

    @PutMapping("/api/admin/landing-page/settings/remaining-spots")
    public ResponseEntity<?> updateRemainingSpots(@RequestBody Map<String, Integer> request) {
        try {
            Integer remainingSpots = request.get("remainingSpots");
            
            if (remainingSpots == null || remainingSpots < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid remaining spots value");
                return ResponseEntity.badRequest().body(error);
            }
            
            Optional<LandingPageSettingsEntity> existingSettings = settingsRepository.findFirstByOrderByUpdatedAtDesc();
            
            LandingPageSettingsEntity settings;
            if (existingSettings.isPresent()) {
                settings = existingSettings.get();
                System.out.println("Updating existing settings with ID: " + settings.getId());
            } else {
                settings = new LandingPageSettingsEntity();
                System.out.println("Creating new settings record");
            }
            
            settings.setRemainingSpots(remainingSpots);
            LandingPageSettingsEntity saved = settingsRepository.save(settings);
            
            System.out.println("Saved remaining spots: " + saved.getRemainingSpots() + " with updatedAt: " + saved.getUpdatedAt());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Remaining spots updated successfully");
            response.put("remainingSpots", saved.getRemainingSpots());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error updating remaining spots: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update remaining spots: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

