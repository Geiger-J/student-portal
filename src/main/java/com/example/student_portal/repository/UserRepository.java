package com.example.student_portal.repository;

import com.example.student_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity.
 * Extends JpaRepository to inherit standard CRUD operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists by email (for registration validation).
     */
    boolean existsByEmail(String email);
}