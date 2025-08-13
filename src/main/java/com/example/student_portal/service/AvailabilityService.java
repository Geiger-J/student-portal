package com.example.student_portal.service;

import com.example.student_portal.entity.AvailabilitySlot;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.Period;
import com.example.student_portal.repository.AvailabilitySlotRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing user availability slots.
 *
 * Responsibilities:
 * - CRUD operations for availability slots
 * - Conflict checking (one slot per user per day/period)
 * - Bulk operations for managing user availability
 */
@Service
@Transactional
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;

    public AvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    /**
     * Get all availability slots for a user.
     */
    public List<AvailabilitySlot> getAvailabilitySlots(User user) {
        return availabilitySlotRepository.findByUser(user);
    }

    /**
     * Add an availability slot for a user.
     * Prevents duplicates for the same user/day/period combination.
     */
    public AvailabilitySlot addAvailabilitySlot(User user, DayOfWeek dayOfWeek, Period period) {
        // Check if slot already exists
        Optional<AvailabilitySlot> existing = availabilitySlotRepository
            .findByUserAndDayOfWeekAndPeriod(user, dayOfWeek, period);
        
        if (existing.isPresent()) {
            return existing.get(); // Return existing slot
        }

        // Create new slot
        AvailabilitySlot slot = new AvailabilitySlot(user, dayOfWeek, period);
        return availabilitySlotRepository.save(slot);
    }

    /**
     * Remove an availability slot for a user.
     */
    public void removeAvailabilitySlot(User user, DayOfWeek dayOfWeek, Period period) {
        Optional<AvailabilitySlot> slot = availabilitySlotRepository
            .findByUserAndDayOfWeekAndPeriod(user, dayOfWeek, period);
        
        if (slot.isPresent()) {
            availabilitySlotRepository.delete(slot.get());
        }
    }

    /**
     * Clear all availability slots for a user.
     */
    public void clearAllAvailability(User user) {
        availabilitySlotRepository.deleteByUser(user);
    }

    /**
     * Check if a user is available at a specific day/period.
     */
    public boolean isUserAvailable(User user, DayOfWeek dayOfWeek, Period period) {
        return availabilitySlotRepository
            .findByUserAndDayOfWeekAndPeriod(user, dayOfWeek, period)
            .isPresent();
    }

    /**
     * Get all users available at a specific day/period.
     */
    public List<AvailabilitySlot> getUsersAvailableAt(DayOfWeek dayOfWeek, Period period) {
        return availabilitySlotRepository.findByDayOfWeekAndPeriod(dayOfWeek, period);
    }
}