package com.example.gitprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "APP_USER")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_seq")
    @SequenceGenerator(name = "app_user_seq", sequenceName = "APP_USER_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME", length = 50, nullable = false, unique = true)
    private String username;

    @JsonIgnore
    @Column(name = "PASSWORD", length = 200, nullable = false)
    private String password;

    @Column(name = "DISPLAY_NAME", length = 100)
    private String displayName;

    @Column(name = "ROLE", length = 20, nullable = false)
    private String role = "DEVELOPER";

    @Column(name = "ACTIVE", nullable = false)
    private Boolean active = true;

    public AppUser() {}

    public AppUser(String username, String password, String displayName, String role) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.active = true;
    }

    public Long    getId()                    { return id; }
    public void    setId(Long id)             { this.id = id; }
    public String  getUsername()              { return username; }
    public void    setUsername(String v)      { this.username = v; }
    public String  getPassword()             { return password; }
    public void    setPassword(String v)     { this.password = v; }
    public String  getDisplayName()          { return displayName; }
    public void    setDisplayName(String v)  { this.displayName = v; }
    public String  getRole()                 { return role; }
    public void    setRole(String v)         { this.role = v; }
    public Boolean getActive()               { return active; }
    public void    setActive(Boolean v)      { this.active = v; }
}