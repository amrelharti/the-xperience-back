package com.thexperience.backend.admin;

import com.thexperience.backend.entities.LandingPageLeadEntity;
import com.thexperience.backend.repos.LandingPageLeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/landing-page/leads")
public class LandingPageLeadController {

    @Autowired
    private LandingPageLeadRepository leadRepository;

    @PostMapping
    public ResponseEntity<?> createLead(@RequestBody Map<String, String> request) {
        try {
            // Validate required fields
            if (request.get("fullName") == null || request.get("fullName").trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Full name is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.get("phone") == null || request.get("phone").trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Phone is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.get("lookingFor") == null || request.get("lookingFor").trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Looking for is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.get("offer") == null || request.get("offer").trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Offer is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create entity
            LandingPageLeadEntity lead = new LandingPageLeadEntity();
            lead.setFullName(request.get("fullName").trim());
            lead.setPhone(request.get("phone").trim());
            lead.setLookingFor(request.get("lookingFor").trim());
            lead.setOffer(request.get("offer").trim());
            
            // Save to database
            LandingPageLeadEntity saved = leadRepository.save(lead);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Lead submitted successfully");
            response.put("id", saved.getId().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("Error saving landing page lead: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to submit lead: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllLeads() {
        try {
            List<LandingPageLeadEntity> leads = leadRepository.findAllByOrderByCreatedAtDesc();
            return ResponseEntity.ok(leads);
        } catch (Exception e) {
            System.err.println("Error fetching landing page leads: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch leads: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

