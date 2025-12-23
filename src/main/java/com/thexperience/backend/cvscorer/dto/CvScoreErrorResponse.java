package com.thexperience.backend.cvscorer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CvScoreErrorResponse {
    private String error;

    public CvScoreErrorResponse() {}

    public CvScoreErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}




