package com.example.student_portal.model;

/**
 * Enum for the status of a tutoring request.
 *
 * OUTSTANDING - Request has not yet been matched. FOUND_TUTOR - For a tutee
 * request, a tutor candidate was identified. FOUND_TUTEE - For a tutor request,
 * a tutee candidate was identified. MATCHED - A final Match was created.
 * REJECTED - Request was rejected or canceled.
 */
public enum RequestStatus {
    OUTSTANDING, FOUND_TUTOR, FOUND_TUTEE, MATCHED, REJECTED, COMPLETED
}