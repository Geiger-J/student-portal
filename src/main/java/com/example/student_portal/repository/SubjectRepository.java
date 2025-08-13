package com.example.student_portal.repository;

import com.example.student_portal.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Subject entity.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByName(String name);
    Subject findByNameIgnoreCase(String name);
    boolean existsByName(String name);
}