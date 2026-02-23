package com.prison.model;

import java.time.LocalDateTime;

public class RecognitionLog {

    private String  personType;   // GUARD / PRISONER / UNKNOWN
    private Integer personId;     // nullable for UNKNOWN
    private String  result;       // RECOGNIZED / FAILED / UNKNOWN
    private LocalDateTime detectedAt;

    // ── New fields ────────────────────────────────────────────────────────────
    private String  personName;      // e.g. "James Carter"
    private String  department;      // e.g. "Block C - Wing 2" or "Security - Gate A"
    private String  cameraLocation;  // e.g. "Main Entrance Cam 3"
    private Double  confidence;      // match confidence percentage, e.g. 92.4

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getPersonType() {
        return personType;
    }

    public void setPersonType(String personType) {
        this.personType = personType;
    }

    public Integer getPersonId() {
        return personId;
    }

    public void setPersonId(Integer personId) {
        this.personId = personId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCameraLocation() {
        return cameraLocation;
    }

    public void setCameraLocation(String cameraLocation) {
        this.cameraLocation = cameraLocation;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}