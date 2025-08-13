package com.example.student_portal.entity;

import com.example.student_portal.model.Period;
import com.example.student_portal.model.Weekday;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;

/**
 * Represents an availability slot for a user.
 * 
 * Each slot represents a specific (day of week, period) combination
 * when a user is available for tutoring sessions.
 * 
 * Enforces unique constraint per user per (day, period).
 */
@Entity
@Table(name = "availability_slots", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "day_of_week", "period"}))
public class AvailabilitySlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who has this availability.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Day of the week for this availability slot.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    /**
     * Period during the day for this availability slot.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    public AvailabilitySlot() {}

    public AvailabilitySlot(User user, DayOfWeek dayOfWeek, Period period) {
        this.user = user;
        this.dayOfWeek = dayOfWeek;
        this.period = period;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AvailabilitySlot that = (AvailabilitySlot) obj;
        
        return user != null && user.equals(that.user) &&
               dayOfWeek == that.dayOfWeek &&
               period == that.period;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(user, dayOfWeek, period);
    }
}