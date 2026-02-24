package com.prison.model;

import java.time.LocalDateTime;

public class RecognitionLog {

    private String        personType;   // GUARD / PRISONER / UNKNOWN
    private Integer       personId;
    private String        result;       // RECOGNIZED / FAILED / UNKNOWN
    private LocalDateTime detectedAt;

    // Joined fields
    private String personName;
    private String department;    // designation/role for guard, cell for prisoner
    private String extraInfo;     // shift for guard, crime for prisoner
    private String contactInfo;   // phone for guard, danger level for prisoner
    private String aadharNumber;
    private String imagePath;

    // Kept for future use (Python integration)
    private Double confidence;
    private String cameraLocation;

    public String  getPersonType()              { return personType; }
    public void    setPersonType(String v)      { personType = v; }

    public Integer getPersonId()               { return personId; }
    public void    setPersonId(Integer v)      { personId = v; }

    public String  getResult()                 { return result; }
    public void    setResult(String v)         { result = v; }

    public LocalDateTime getDetectedAt()           { return detectedAt; }
    public void          setDetectedAt(LocalDateTime v) { detectedAt = v; }

    public String  getPersonName()             { return personName; }
    public void    setPersonName(String v)     { personName = v; }

    public String  getDepartment()             { return department; }
    public void    setDepartment(String v)     { department = v; }

    public String  getExtraInfo()              { return extraInfo; }
    public void    setExtraInfo(String v)      { extraInfo = v; }

    public String  getContactInfo()            { return contactInfo; }
    public void    setContactInfo(String v)    { contactInfo = v; }

    public String  getAadharNumber()           { return aadharNumber; }
    public void    setAadharNumber(String v)   { aadharNumber = v; }

    public String  getImagePath()              { return imagePath; }
    public void    setImagePath(String v)      { imagePath = v; }

    public Double  getConfidence()             { return confidence; }
    public void    setConfidence(Double v)     { confidence = v; }

    public String  getCameraLocation()         { return cameraLocation; }
    public void    setCameraLocation(String v) { cameraLocation = v; }
}