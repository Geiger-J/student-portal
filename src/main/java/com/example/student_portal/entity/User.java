package com.example.student_portal.entity;

import java.util.HashSet;
import java.util.Set;

import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.Role;
import com.example.student_portal.model.YearGroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Represents a user in the Student Portal.
 *
 * A user can be a student or staff member (admin). Students can both request
 * tutoring and offer tutoring.
 */
@Entity
@Table(name = "users")
public class User {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Full name to display in UI.
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(nullable = false)
    private String fullName;

    /**
     * School email address (enforced domain).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@bromsgrove-school\\.co\\.uk$", message = "Email must be from bromsgrove-school.co.uk domain")
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt-hashed password. Never store raw passwords.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Application role: STUDENT or ADMIN. Determines what access the user has to
     * administrative features.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    /**
     * Year group for students. Required for all students.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private YearGroup yearGroup;

    /**
     * Exam board for sixth form (Year 12 & 13). For years below sixth form, this
     * should be NONE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_board", nullable = false)
    private ExamBoard examBoard = ExamBoard.NONE;

    /**
     * Subjects this user studies or can tutor.
     */
    @ManyToMany
    @JoinTable(name = "user_subjects", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<Subject> subjects = new HashSet<>();

    /**
     * Timeslots this user is available for tutoring sessions.
     */
    @ManyToMany
    @JoinTable(name = "user_timeslots", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "timeslot_id"))
    private Set<Timeslot> availableTimeslots = new HashSet<>();

    public User() {}

    // Getters and setters
    public Long getId() { return id; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email.toLowerCase(); }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }

    public void setRole(Role role) { this.role = role; }

    public YearGroup getYearGroup() { return yearGroup; }

    public void setYearGroup(YearGroup yearGroup) { this.yearGroup = yearGroup; }

    public ExamBoard getExamBoard() { return examBoard; }

    public void setExamBoard(ExamBoard examBoard) { this.examBoard = examBoard; }

    public Set<Subject> getSubjects() { return subjects; }

    public void setSubjects(Set<Subject> subjects) { this.subjects = subjects; }

    public Set<Timeslot> getAvailableTimeslots() { return availableTimeslots; }

    public void setAvailableTimeslots(Set<Timeslot> availableTimeslots) { this.availableTimeslots = availableTimeslots; }
}
