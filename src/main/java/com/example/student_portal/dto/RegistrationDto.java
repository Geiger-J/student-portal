package com.example.student_portal.dto;

import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.YearGroup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationDto {

    @NotBlank
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank
    @Email
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@bromsgrove-school\\.co\\.uk$", message = "Email must be from bromsgrove-school.co.uk domain")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotNull
    private YearGroup yearGroup;

    @NotNull
    private ExamBoard examBoard;

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public YearGroup getYearGroup() { return yearGroup; }

    public void setYearGroup(YearGroup yearGroup) { this.yearGroup = yearGroup; }

    public ExamBoard getExamBoard() { return examBoard; }

    public void setExamBoard(ExamBoard examBoard) { this.examBoard = examBoard; }
}