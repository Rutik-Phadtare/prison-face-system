package com.prison.model;

import java.time.LocalDateTime;

public class RecognitionLog {

    private String personType;   // GUARD / PRISONER / UNKNOWN
    private Integer personId;    // nullable for UNKNOWN
    private String result;       // RECOGNIZED / UNKNOWN

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
    private LocalDateTime detectedAt;

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

}
