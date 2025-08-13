package com.example.student_portal.entity;

import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.model.YearGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tutoring request in the system.
 *
 * A request can be:
 * - A tutor offering help in a subject
 * - A tutee requesting help in a subject
 *
 * Each request can include multiple possible timeslots for matching flexibility.
 */
@Entity
@Table(name = "requests")
public class Request {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who created this request.
     * Many requests can be made by the same user.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Subject for this request (single subject for simplicity).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * Possible timeslots when the tutoring could happen.
     * Many-to-many to allow multiple options.
     */
    @ManyToMany
    @JoinTable(
        name = "request_timeslots",
        joinColumns = @JoinColumn(name = "request_id"),
        inverseJoinColumns = @JoinColumn(name = "timeslot_id")
    )
    private Set<Timeslot> possibleTimeslots = new HashSet<>();

    /**
     * Type of request: TUTOR or TUTEE.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;

    /**
     * Current status of this request in the matching pipeline.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.OUTSTANDING;

    /**
     * Year group this request targets.
     * - For TUTOR requests, the tutor's year group (user's year group) is stored for convenience.
     * - For TUTEE requests, the tutee's year group is stored.
     * This can be used to enforce "tutor's year >= tutee's year".
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "year_group", nullable = false)
    private YearGroup yearGroup;

    public Request() { }

    // Getters and setters

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Set<Timeslot> getPossibleTimeslots() { return possibleTimeslots; }
    public void setPossibleTimeslots(Set<Timeslot> possibleTimeslots) { this.possibleTimeslots = possibleTimeslots; }

    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public YearGroup getYearGroup() { return yearGroup; }
    public void setYearGroup(YearGroup yearGroup) { this.yearGroup = yearGroup; }
}