package com.example.student_portal.model;

/**
 * Application roles.
 * Stored in the database as strings (e.g. "STUDENT", "ADMIN"),
 * and mapped to Spring Security authorities "ROLE_STUDENT" / "ROLE_ADMIN".
 */
public enum Role {
    STUDENT,
    ADMIN
}