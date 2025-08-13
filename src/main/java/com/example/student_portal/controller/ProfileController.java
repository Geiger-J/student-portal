package com.example.student_portal.controller;

import com.example.student_portal.entity.User;
import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.model.TeachingMode;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;
import com.example.student_portal.service.AvailabilityService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Enhanced profile controller for managing user profile including new fields:
 * - maxSessionsPerWeek
 * - teachingMode preference
 * - availability management through AvailabilityService
 *
 * Provides comprehensive profile management while maintaining backwards compatibility.
 */
@Controller
public class ProfileController {

    private final UserService userService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;
    private final AvailabilityService availabilityService;

    public ProfileController(UserService userService,
                           SubjectService subjectService,
                           TimeslotService timeslotService,
                           AvailabilityService availabilityService) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
        this.availabilityService = availabilityService;
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("yearGroups", YearGroup.values());
        model.addAttribute("examBoards", ExamBoard.values());
        model.addAttribute("teachingModes", TeachingMode.values());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("availabilitySlots", availabilityService.getAvailabilitySlots(user));

        return "profile";
    }

    /**
     * Enhanced profile update to handle new fields including maxSessionsPerWeek and teachingMode.
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam("fullName") String fullName,
                              @RequestParam("yearGroup") String yearGroup,
                              @RequestParam("examBoard") String examBoard,
                              @RequestParam(value = "maxSessionsPerWeek", defaultValue = "3") Integer maxSessionsPerWeek,
                              @RequestParam(value = "teachingMode", defaultValue = "IN_PERSON") String teachingMode,
                              Model model) {
        User user = userService.findByEmail(principal.getUsername());

        // Update basic fields
        user.setFullName(fullName);
        user.setYearGroup(YearGroup.valueOf(yearGroup));
        user.setMaxSessionsPerWeek(maxSessionsPerWeek);
        user.setTeachingMode(TeachingMode.valueOf(teachingMode));

        // Enforce exam board constraints
        switch (user.getYearGroup()) {
            case YEAR_12, YEAR_13 -> user.setExamBoard(ExamBoard.valueOf(examBoard));
            default -> user.setExamBoard(ExamBoard.NONE);
        }

        // Validate maxSessionsPerWeek range
        if (maxSessionsPerWeek < 1 || maxSessionsPerWeek > 10) {
            model.addAttribute("errorMessage", "Max sessions per week must be between 1 and 10");
            prepareModelForProfileView(user, model);
            return "profile";
        }

        userService.save(user);

        model.addAttribute("successMessage", "Profile updated successfully!");
        prepareModelForProfileView(user, model);
        return "profile";
    }

    /**
     * Helper method to prepare model attributes for profile view.
     */
    private void prepareModelForProfileView(User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("yearGroups", YearGroup.values());
        model.addAttribute("examBoards", ExamBoard.values());
        model.addAttribute("teachingModes", TeachingMode.values());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("availabilitySlots", availabilityService.getAvailabilitySlots(user));
    }
}