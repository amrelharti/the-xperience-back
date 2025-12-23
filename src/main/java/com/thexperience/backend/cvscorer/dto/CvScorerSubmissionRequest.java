package com.thexperience.backend.cvscorer.dto;

public class CvScorerSubmissionRequest {
    private String name;
    private String email;
    private String phone;
    private Integer finalScore;
    
    public CvScorerSubmissionRequest() {
    }
    
    public CvScorerSubmissionRequest(String name, String email, String phone, Integer finalScore) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.finalScore = finalScore;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Integer getFinalScore() {
        return finalScore;
    }
    
    public void setFinalScore(Integer finalScore) {
        this.finalScore = finalScore;
    }
}

