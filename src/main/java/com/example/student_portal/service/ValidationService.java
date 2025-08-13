package com.example.student_portal.service;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.entity.Subject;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.repository.RequestRepository;
import org.springframework.stereotype.Service;

/**
 * Service for business rule validation and enforcement.
 * 
 * Centralizes complex business logic that goes beyond simple field validation.
 * Used by controllers and services to ensure business rules are consistently enforced.
 */
@Service
public class ValidationService {

    private final RequestRepository requestRepository;

    public ValidationService(RequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Validates that a user can create a request of the specified type and subject.
     * 
     * @param user The user creating the request
     * @param subject The subject for the request
     * @param requestType The type of request (TUTOR or TUTEE)
     * @throws IllegalArgumentException if validation fails
     */
    public void validateRequestCreation(User user, Subject subject, RequestType requestType) {
        // Check profile completeness
        if (!isUserProfileComplete(user)) {
            throw new IllegalArgumentException("Profile must be completed before creating requests. " +
                "Please set your availability, select subjects, and complete your profile information.");
        }

        // Check if user has selected this subject
        if (!user.getSubjects().contains(subject)) {
            throw new IllegalArgumentException("You must add " + subject.getName() + 
                " to your subjects before creating a request for it. Visit your Subjects page to add it.");
        }

        // Check for duplicate outstanding requests
        boolean hasOutstandingRequest = requestRepository.findByUser(user)
            .stream()
            .anyMatch(r -> r.getSubject().equals(subject) && 
                          r.getType() == requestType && 
                          r.getStatus() == RequestStatus.OUTSTANDING);

        if (hasOutstandingRequest) {
            throw new IllegalArgumentException("You already have an outstanding " + 
                requestType.name().toLowerCase() + " request for " + subject.getName() + 
                ". Please cancel it first if you want to create a new one.");
        }

        // Validate tutor eligibility for the subject
        if (requestType == RequestType.TUTOR) {
            validateTutorEligibility(user, subject);
        }
    }

    /**
     * Validates that a user can act as a tutor for a specific subject.
     */
    public void validateTutorEligibility(User user, Subject subject) {
        // For now, we only check that they have the subject in their list
        // In a full implementation, we might check grades, teacher approval, etc.
        if (!user.getSubjects().contains(subject)) {
            throw new IllegalArgumentException("You must have " + subject.getName() + 
                " in your subjects list to tutor it.");
        }

        // Check minimum availability for tutors
        if (user.getAvailabilitySlots().isEmpty()) {
            throw new IllegalArgumentException("Tutors must set their availability before offering tutoring. " +
                "Please visit the Availability page to set your available times.");
        }

        // Check reasonable session limit
        if (user.getMaxSessionsPerWeek() == null || user.getMaxSessionsPerWeek() < 1) {
            throw new IllegalArgumentException("Tutors must specify how many sessions per week they can handle. " +
                "Please update your profile to set this value.");
        }
    }

    /**
     * Validates that a tutor can teach a tutee based on year group constraints.
     */
    public void validateTutorTuteeYearGroupCompatibility(User tutor, User tutee) {
        YearGroup tutorYear = tutor.getYearGroup();
        YearGroup tuteeYear = tutee.getYearGroup();

        if (tutorYear == null || tuteeYear == null) {
            throw new IllegalArgumentException("Both tutor and tutee must have year groups set");
        }

        if (tutorYear.ordinal() < tuteeYear.ordinal()) {
            throw new IllegalArgumentException("Tutors must be in the same or higher year group than tutees. " +
                "Tutor is in " + tutorYear + " but tutee is in " + tuteeYear + ".");
        }
    }

    /**
     * Checks if a user's profile is sufficiently complete for tutoring activities.
     */
    public boolean isUserProfileComplete(User user) {
        // Check required fields
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return false;
        }

        if (user.getYearGroup() == null) {
            return false;
        }

        if (user.getExamBoard() == null) {
            return false;
        }

        // Check subjects - at least one required
        if (user.getSubjects().isEmpty()) {
            return false;
        }

        // Check availability - at least one slot required
        if (user.getAvailabilitySlots().isEmpty()) {
            return false;
        }

        // Check max sessions per week is set
        if (user.getMaxSessionsPerWeek() == null || user.getMaxSessionsPerWeek() < 1) {
            return false;
        }

        return true;
    }

    /**
     * Calculates profile completion percentage for user feedback.
     */
    public int calculateProfileCompleteness(User user) {
        int score = 0;
        int maxScore = 100;

        // Basic profile fields (40 points)
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            score += 15;
        }
        if (user.getYearGroup() != null) {
            score += 15;
        }
        if (user.getExamBoard() != null) {
            score += 10;
        }

        // Subjects (20 points)
        if (user.getSubjects() != null && !user.getSubjects().isEmpty()) {
            score += 20;
        }

        // Availability (25 points)
        if (user.getAvailabilitySlots() != null && !user.getAvailabilitySlots().isEmpty()) {
            score += 25;
        }

        // Max sessions per week set (15 points)
        if (user.getMaxSessionsPerWeek() != null && user.getMaxSessionsPerWeek() > 0) {
            score += 15;
        }

        return Math.min(score, maxScore);
    }

    /**
     * Validates request data before processing.
     */
    public void validateRequest(Request request) {
        if (request.getUser() == null) {
            throw new IllegalArgumentException("Request must have a user");
        }

        if (request.getSubject() == null) {
            throw new IllegalArgumentException("Request must have a subject");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Request must have a type");
        }

        if (request.getPossibleTimeslots() == null || request.getPossibleTimeslots().isEmpty()) {
            throw new IllegalArgumentException("Request must have at least one possible timeslot");
        }

        if (request.getYearGroup() == null) {
            throw new IllegalArgumentException("Request must have a year group");
        }

        // Validate that timeslots are available for the user
        if (request.getType() == RequestType.TUTOR) {
            validateTutorTimeslots(request);
        }
    }

    /**
     * Validates that a tutor request's timeslots match the user's availability.
     */
    private void validateTutorTimeslots(Request request) {
        User tutor = request.getUser();
        
        // Check that at least one requested timeslot matches tutor availability
        boolean hasMatchingSlot = request.getPossibleTimeslots()
            .stream()
            .anyMatch(timeslot -> tutor.getAvailableTimeslots().contains(timeslot));

        if (!hasMatchingSlot) {
            throw new IllegalArgumentException("Tutor requests must include timeslots where you are available. " +
                "Please update your availability or select different timeslots.");
        }
    }
}