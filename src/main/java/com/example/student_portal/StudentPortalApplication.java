package com.example.student_portal;

// Spring Boot core imports
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Enable scheduling for the weekly matching job
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Student Matching App.
 *
 * Responsibilities:
 * - Boots the Spring application context
 * - Auto-configures web, JPA, and security components
 * - Enables scheduled tasks (weekly matching algorithm)
 */
@SpringBootApplication
@EnableScheduling
public class StudentPortalApplication {

    /**
     * Standard Java main method that launches the Spring Boot application.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SpringApplication.run(StudentPortalApplication.class, args);
    }
}