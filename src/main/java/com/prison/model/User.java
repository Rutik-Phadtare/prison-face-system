package com.prison.model;

public class User {

    private int     userId;
    private String  username;
    private String  passwordHash;
    private String  role;
    private String  displayName;
    private boolean active = true;
    private String  createdBy;       // ← was missing

    public int    getUserId()                    { return userId; }
    public void   setUserId(int userId)          { this.userId = userId; }

    public String getUsername()                  { return username; }
    public void   setUsername(String username)   { this.username = username; }

    public String getPasswordHash()              { return passwordHash; }
    public void   setPasswordHash(String p)      { this.passwordHash = p; }

    public String getRole()                      { return role; }
    public void   setRole(String role)           { this.role = role; }

    public String getDisplayName()               { return displayName; }
    public void   setDisplayName(String d)       { this.displayName = d; }

    public boolean isActive()                    { return active; }
    public void    setActive(boolean a)          { this.active = a; }

    public String getCreatedBy()                 { return createdBy; }
    public void   setCreatedBy(String c)         { this.createdBy = c; }
}