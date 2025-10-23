package com.thexperience.backend.homework.dto;

public class ScoreHomeworkRequest {
    private Integer value;

    // Constructors
    public ScoreHomeworkRequest() {}

    public ScoreHomeworkRequest(Integer value) {
        this.value = value;
    }

    // Getters and setters
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
}
