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

    // 🔥 Added New Fields (Previous Update)
    private int age;
    private LocalDate birthDate;
    private String address;
    private String gender;
    private String transferFrom;
    private double salary;

    // ✨ Database Sync Fields (Latest Update)
    private String aadharNumber;
    private String phoneNumber;
    private String batchId;
    private String email;

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

    // 🔥 Getters/Setters (Previous Update)
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getTransferFrom() { return transferFrom; }
    public void setTransferFrom(String transferFrom) { this.transferFrom = transferFrom; }
    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    // ✨ Getters/Setters (Latest Update)
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}