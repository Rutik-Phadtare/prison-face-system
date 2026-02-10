package com.prison.model;

import java.time.LocalDate;

public class Prisoner {

    private int prisonerId;
    private String name;
    private String crime;
    private String cellNo;
    private int sentenceYears;
    private String status;


    /* 🔴 NEW FIELDS */
    private String description;
    private LocalDate releaseDate;

    private LocalDate sentenceStartDate;


    /* =========================
       GETTERS & SETTERS
       ========================= */
    public LocalDate getSentenceStartDate() { return sentenceStartDate; }
    public void setSentenceStartDate(LocalDate d) { this.sentenceStartDate = d; }

    public int getPrisonerId() {
        return prisonerId;
    }

    public void setPrisonerId(int prisonerId) {
        this.prisonerId = prisonerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCrime() { return crime; }


    public void setCrime(String crime) {
        this.crime = crime;
    }

    public String getCellNo() {
        return cellNo;
    }

    public void setCellNo(String cellNo) {
        this.cellNo = cellNo;
    }

    public int getSentenceYears() {
        return sentenceYears;
    }

    public void setSentenceYears(int sentenceYears) {
        this.sentenceYears = sentenceYears;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /* =========================
       🔴 NEW GETTERS & SETTERS
       ========================= */

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }


    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate d) { this.releaseDate = d; }
}
