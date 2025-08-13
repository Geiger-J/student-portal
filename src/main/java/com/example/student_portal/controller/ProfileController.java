package com.example.student_portal.controller;

import com.example.student_portal.entity.User;
import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Profile controller allows the logged-in user to view and update their profile.
 *
 * Note: For simplicity, this example shows dropdowns for enums and multi-selects for
 * subjects and timeslots (requires minimal Thymeleaf binding).
 */
@Controller
public class ProfileController {

    private final UserService userService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;

    public ProfileController(UserService userService,
                             SubjectService subjectService,
                             TimeslotService timeslotService) {
        this.userService = userService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("yearGroups", YearGroup.values());
        model.addAttribute("examBoards", ExamBoard.values());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());

        return "profile";
    }

    /**
     * Updates basic profile fields. In a production app, you'd handle binding of
     * subjects/timeslots with IDs from the form and map them back (omitted here for brevity).
     */
    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam("fullName") String fullName,
                                @RequestParam("yearGroup") String yearGroup,
                                @RequestParam("examBoard") String examBoard,
                                Model model) {
        User user = userService.findByEmail(principal.getUsername());

        user.setFullName(fullName);
        user.setYearGroup(YearGroup.valueOf(yearGroup));

        // Enforce NONE for non-sixth-form years
        switch (user.getYearGroup()) {
            case YEAR_12, YEAR_13 -> user.setExamBoard(ExamBoard.valueOf(examBoard));
            default -> user.setExamBoard(ExamBoard.NONE);
        }

        userService.save(user);

        model.addAttribute("user", user);
        model.addAttribute("yearGroups", YearGroup.values());
        model.addAttribute("examBoards", ExamBoard.values());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("successMessage", "Profile updated successfully!");

        return "profile";
    }
}