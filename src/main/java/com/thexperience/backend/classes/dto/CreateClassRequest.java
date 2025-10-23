package com.thexperience.backend.classes.dto;

import java.time.LocalDate;
import java.util.UUID;

public class CreateClassRequest {
    private String name;
    private String description;
    private UUID moduleId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxStudents;

    // Constructors
    public CreateClassRequest() {}

    public CreateClassRequest(String name, String description, UUID moduleId, 
                             LocalDate startDate, LocalDate endDate, Integer maxStudents) {
        this.name = name;
        this.description = description;
        this.moduleId = moduleId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxStudents = maxStudents;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getModuleId() { return moduleId; }
    public void setModuleId(UUID moduleId) { this.moduleId = moduleId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }
}


