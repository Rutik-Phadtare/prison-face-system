package com.prison.model;

import java.time.LocalDate;

public class Prisoner {

    // ── Core (existing) ────────────────────────────────────────────────────
    private int       prisonerId;
    private String    name;
    private String    crime;
    private String    cellNo;
    private int       sentenceYears;
    private String    status;
    private String    description;
    private LocalDate releaseDate;
    private LocalDate sentenceStartDate;

    // ── Personal details ───────────────────────────────────────────────────
    private int    age;
    private String gender;
    private String nationality;
    private String homeAddress;
    private String aadharNumber;
    private String bloodType;         // A+, B-, O+, etc.
    private String height;            // stored as text e.g. "5'10\""
    private String weight;            // stored as text e.g. "72 kg"
    private String identificationMarks; // scars, tattoos, etc.

    // ── Contact / legal ────────────────────────────────────────────────────
    private String emergencyContact;  // next-of-kin name
    private String emergencyPhone;
    private String lawyerName;
    private String lawyerPhone;

    // ── Classification ─────────────────────────────────────────────────────
    /** LOW · MEDIUM · HIGH · MAXIMUM */
    private String dangerLevel;

    // ── Behavioral / monitoring ────────────────────────────────────────────
    /** EXCELLENT · GOOD · FAIR · POOR */
    private String behaviorRating;
    private String incidentNotes;     // free-text incident / disciplinary log
    private String visitorLog;        // free-text visitor / call / tablet log

    // ══════════════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ══════════════════════════════════════════════════════════════════════

    // Core
    public int       getPrisonerId()           { return prisonerId; }
    public void      setPrisonerId(int v)      { prisonerId = v; }

    public String    getName()                 { return name; }
    public void      setName(String v)         { name = v; }

    public String    getCrime()                { return crime; }
    public void      setCrime(String v)        { crime = v; }

    public String    getCellNo()               { return cellNo; }
    public void      setCellNo(String v)       { cellNo = v; }

    public int       getSentenceYears()        { return sentenceYears; }
    public void      setSentenceYears(int v)   { sentenceYears = v; }

    public String    getStatus()               { return status; }
    public void      setStatus(String v)       { status = v; }

    public String    getDescription()          { return description; }
    public void      setDescription(String v)  { description = v; }

    public LocalDate getReleaseDate()          { return releaseDate; }
    public void      setReleaseDate(LocalDate v) { releaseDate = v; }

    public LocalDate getSentenceStartDate()    { return sentenceStartDate; }
    public void      setSentenceStartDate(LocalDate v) { sentenceStartDate = v; }

    // Personal
    public int    getAge()                     { return age; }
    public void   setAge(int v)                { age = v; }

    public String getGender()                  { return gender; }
    public void   setGender(String v)          { gender = v; }

    public String getNationality()             { return nationality; }
    public void   setNationality(String v)     { nationality = v; }

    public String getHomeAddress()             { return homeAddress; }
    public void   setHomeAddress(String v)     { homeAddress = v; }

    public String getAadharNumber()            { return aadharNumber; }
    public void   setAadharNumber(String v)    { aadharNumber = v; }

    public String getBloodType()               { return bloodType; }
    public void   setBloodType(String v)       { bloodType = v; }

    public String getHeight()                  { return height; }
    public void   setHeight(String v)          { height = v; }

    public String getWeight()                  { return weight; }
    public void   setWeight(String v)          { weight = v; }

    public String getIdentificationMarks()     { return identificationMarks; }
    public void   setIdentificationMarks(String v) { identificationMarks = v; }

    // Contact / legal
    public String getEmergencyContact()        { return emergencyContact; }
    public void   setEmergencyContact(String v){ emergencyContact = v; }

    public String getEmergencyPhone()          { return emergencyPhone; }
    public void   setEmergencyPhone(String v)  { emergencyPhone = v; }

    public String getLawyerName()              { return lawyerName; }
    public void   setLawyerName(String v)      { lawyerName = v; }

    public String getLawyerPhone()             { return lawyerPhone; }
    public void   setLawyerPhone(String v)     { lawyerPhone = v; }

    // Classification
    public String getDangerLevel()             { return dangerLevel; }
    public void   setDangerLevel(String v)     { dangerLevel = v; }

    // Behavioral
    public String getBehaviorRating()          { return behaviorRating; }
    public void   setBehaviorRating(String v)  { behaviorRating = v; }

    public String getIncidentNotes()           { return incidentNotes; }
    public void   setIncidentNotes(String v)   { incidentNotes = v; }

    public String getVisitorLog()              { return visitorLog; }
    public void   setVisitorLog(String v)      { visitorLog = v; }
}