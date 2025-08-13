package com.example.student_portal.repository;

import com.example.student_portal.entity.Timeslot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Timeslot entity.
 */
@Repository
public interface TimeslotRepository extends JpaRepository<Timeslot, Long> {
    Optional<Timeslot> findByLabel(String label);
    boolean existsByLabel(String label);
}