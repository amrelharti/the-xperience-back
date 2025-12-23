package com.thexperience.backend.cvscorer.controller;

import com.thexperience.backend.cvscorer.dto.CvScoreErrorResponse;
import com.thexperience.backend.cvscorer.dto.CvScoreResponse;
import com.thexperience.backend.cvscorer.service.CvScoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/score-cv")
public class CvScoringController {

    @Autowired
    private CvScoringService cvScoringService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> scoreCv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "targetRole", required = false) String targetRole,
            @RequestParam(value = "feedbackLanguage", required = false) String feedbackLanguage) {
        
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                CvScoreErrorResponse error = new CvScoreErrorResponse("PDF file is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                CvScoreErrorResponse error = new CvScoreErrorResponse("File must be a PDF");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Normalize job description - handle null and trim
            if (jobDescription != null) {
                jobDescription = jobDescription.trim();
                if (jobDescription.isEmpty()) {
                    jobDescription = null;
                }
            }
            
            // Normalize target role - handle null and trim
            if (targetRole != null) {
                targetRole = targetRole.trim();
                if (targetRole.isEmpty()) {
                    targetRole = null;
                }
            }
            
            // Validate language parameter
            if (feedbackLanguage != null && !feedbackLanguage.trim().isEmpty()) {
                String lang = feedbackLanguage.toUpperCase().trim();
                if (!lang.equals("FRENCH") && !lang.equals("ENGLISH") && !lang.equals("AUTO")) {
                    CvScoreErrorResponse error = new CvScoreErrorResponse("feedbackLanguage must be 'FRENCH', 'ENGLISH', or 'AUTO'");
                    return ResponseEntity.badRequest().body(error);
                }
                feedbackLanguage = lang;
            }
            
            // Log input parameters for debugging (without sensitive data)
            System.out.println("CV Scoring Request - Target Role: " + (targetRole != null ? targetRole : "null"));
            System.out.println("CV Scoring Request - Job Description length: " + (jobDescription != null ? jobDescription.length() : 0));
            if (jobDescription != null && jobDescription.length() > 0) {
                System.out.println("CV Scoring Request - Job Description preview: " + jobDescription.substring(0, Math.min(100, jobDescription.length())));
            }
            System.out.println("CV Scoring Request - Feedback Language: " + feedbackLanguage);
            
            // Score the CV
            Object response = cvScoringService.scoreCv(file, jobDescription, targetRole, feedbackLanguage);
            
            // Check if it's an error response
            if (response instanceof CvScoreErrorResponse) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("CV Scoring IllegalArgumentException: " + e.getMessage());
            e.printStackTrace();
            CvScoreErrorResponse error = new CvScoreErrorResponse(e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            System.err.println("CV Scoring Exception: " + e.getMessage());
            e.printStackTrace();
            CvScoreErrorResponse error = new CvScoreErrorResponse("Failed to score CV: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @PostMapping(value = "/extract-text", consumes = "multipart/form-data")
    public ResponseEntity<?> extractText(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "PDF file is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File must be a PDF");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Extract text
            String extractedText = cvScoringService.extractTextFromPdfFile(file);
            
            Map<String, String> response = new HashMap<>();
            response.put("text", extractedText);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to extract text from PDF: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

