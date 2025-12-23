package com.thexperience.backend.cvscorer.controller;

import com.thexperience.backend.cvscorer.dto.CvScorerSubmissionRequest;
import com.thexperience.backend.entities.CvScorerSubmissionEntity;
import com.thexperience.backend.repos.CvScorerSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cv-scorer/submissions")
public class CvScorerSubmissionController {

    @Autowired
    private CvScorerSubmissionRepository submissionRepository;

    @PostMapping
    public ResponseEntity<?> submitContactInfo(@RequestBody CvScorerSubmissionRequest request) {
        try {
            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Name is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Phone is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Create entity
            CvScorerSubmissionEntity submission = new CvScorerSubmissionEntity();
            submission.setName(request.getName().trim());
            submission.setEmail(request.getEmail().trim().toLowerCase());
            submission.setPhone(request.getPhone().trim());
            submission.setFinalScore(request.getFinalScore());
            
            // Save to database
            CvScorerSubmissionEntity saved = submissionRepository.save(submission);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Contact information submitted successfully");
            response.put("id", saved.getId().toString());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            System.err.println("Error saving CV scorer submission: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to submit contact information: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllSubmissions() {
        try {
            java.util.List<CvScorerSubmissionEntity> submissions = submissionRepository.findAll();
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            System.err.println("Error fetching CV scorer submissions: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch submissions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

