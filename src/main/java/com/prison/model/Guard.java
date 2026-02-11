package com.prison.model;

import java.time.LocalDate;

public class Guard {
    private int guardId;
    private String name;
    private String designation;
    private String shift;
    private String status;
    private LocalDate joiningDate;
    private String description;

    // Getters and Setters
    public int getGuardId() { return guardId; }
    public void setGuardId(int guardId) { this.guardId = guardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}