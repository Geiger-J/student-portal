package com.example.student_portal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a school subject such as "Mathematics", "Physics", or "English Literature".
 *
 * Subjects are pre-populated using data.sql at startup.
 */
@Entity
@Table(name = "subjects")
public class Subject {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Subject name (unique, required).
     */
    @NotBlank(message = "Subject name is required")
    @Size(min = 2, max = 100, message = "Subject name must be between 2 and 100 characters")
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Users linked to this subject (inverse side of User.subjects).
     */
    @ManyToMany(mappedBy = "subjects")
    private Set<User> users = new HashSet<>();

    /**
     * Requests that involve this subject (inverse side of Request.subject).
     */
    @OneToMany(mappedBy = "subject")
    private Set<Request> requests = new HashSet<>();

    public Subject() { }

    public Subject(String name) {
        this.name = name;
    }

    // Getters and setters

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

    public Set<Request> getRequests() { return requests; }
    public void setRequests(Set<Request> requests) { this.requests = requests; }
}