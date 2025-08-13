package com.example.student_portal.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a single available timeslot in the school timetable.
 *
 * Example labels: "Monday Period 1", "Wednesday Period 5"
 * We store as an entity (not enum) to allow dynamic management.
 */
@Entity
@Table(name = "timeslots")
public class Timeslot {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Label of the timeslot, e.g. "Monday Period 1".
     * We enforce a simple pattern "[Weekday] Period [1-7]".
     */
    @NotBlank(message = "Timeslot label is required")
    @Size(min = 5, max = 50, message = "Timeslot label must be between 5 and 50 characters")
    @Pattern(
        regexp = "^(Monday|Tuesday|Wednesday|Thursday|Friday) Period [1-7]$",
        message = "Timeslot must match format 'Day Period X' where X is 1–7"
    )
    @Column(nullable = false, unique = true)
    private String label;

    /**
     * Users who are available in this timeslot (inverse side).
     */
    @ManyToMany(mappedBy = "availableTimeslots")
    private Set<User> users = new HashSet<>();

    /**
     * Requests that include this timeslot as a possible meeting time (inverse side).
     */
    @ManyToMany(mappedBy = "possibleTimeslots")
    private Set<Request> requests = new HashSet<>();

    public Timeslot() { }

    public Timeslot(String label) {
        this.label = label;
    }

    // Getters and setters

    public Long getId() { return id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

    public Set<Request> getRequests() { return requests; }
    public void setRequests(Set<Request> requests) { this.requests = requests; }
}