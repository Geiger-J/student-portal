package com.example.student_portal.repository;

import com.example.student_portal.entity.AvailabilitySlot;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.Period;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing AvailabilitySlot entities.
 */
@Repository
public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    
    /**
     * Find all availability slots for a specific user.
     */
    List<AvailabilitySlot> findByUser(User user);
    
    /**
     * Find a specific availability slot by user, day, and period.
     */
    Optional<AvailabilitySlot> findByUserAndDayOfWeekAndPeriod(User user, DayOfWeek dayOfWeek, Period period);
    
    /**
     * Find all availability slots for a specific day and period.
     */
    List<AvailabilitySlot> findByDayOfWeekAndPeriod(DayOfWeek dayOfWeek, Period period);
    
    /**
     * Delete all availability slots for a user.
     */
    void deleteByUser(User user);
}