package com.example.student_portal.entity;

import jakarta.persistence.*;

/**
 * Represents a confirmed tutoring match between a tutor request and a tutee request.
 *
 * Created by the matching algorithm when:
 *  - Same subject
 *  - Overlapping timeslot
 *  - Tutor's year group >= Tutee's year group
 *  - Exam board compatibility for sixth form (if applicable)
 */
@Entity
@Table(name = "matches")
public class Match {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Each match links exactly one tutor request.
    @ManyToOne(optional = false)
    @JoinColumn(name = "tutor_request_id", nullable = false)
    private Request tutorRequest;

    // Each match links exactly one tutee request.
    @ManyToOne(optional = false)
    @JoinColumn(name = "tutee_request_id", nullable = false)
    private Request tuteeRequest;

    // Assigned timeslot that both can attend.
    @ManyToOne(optional = false)
    @JoinColumn(name = "timeslot_id", nullable = false)
    private Timeslot matchedTimeslot;

    // Status of this match (active/cancelled/completed). Keep simple text for now.
    @Column(nullable = false)
    private String status = "ACTIVE";

    public Match() { }

    public Match(Request tutorRequest, Request tuteeRequest, Timeslot matchedTimeslot) {
        this.tutorRequest = tutorRequest;
        this.tuteeRequest = tuteeRequest;
        this.matchedTimeslot = matchedTimeslot;
        this.status = "ACTIVE";
    }

    // Getters and setters

    public Long getId() { return id; }

    public Request getTutorRequest() { return tutorRequest; }
    public void setTutorRequest(Request tutorRequest) { this.tutorRequest = tutorRequest; }

    public Request getTuteeRequest() { return tuteeRequest; }
    public void setTuteeRequest(Request tuteeRequest) { this.tuteeRequest = tuteeRequest; }

    public Timeslot getMatchedTimeslot() { return matchedTimeslot; }
    public void setMatchedTimeslot(Timeslot matchedTimeslot) { this.matchedTimeslot = matchedTimeslot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}