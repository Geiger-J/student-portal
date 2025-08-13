package com.example.student_portal.controller;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Admin dashboard controller.
 * Restricted to users with ADMIN role.
 */
@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RequestService requestService;
    private final MatchService matchService;

    public AdminController(UserService userService,
                           RequestService requestService,
                           MatchService matchService) {
        this.userService = userService;
        this.requestService = requestService;
        this.matchService = matchService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        List<User> users = userService.findAllUsers();
        List<Request> outstandingTutors = requestService.getOutstandingTutorRequests();
        List<Request> outstandingTutees = requestService.getOutstandingTuteeRequests();
        List<Match> matches = matchService.findAllMatches();

        model.addAttribute("users", users);
        model.addAttribute("outstandingTutors", outstandingTutors);
        model.addAttribute("outstandingTutees", outstandingTutees);
        model.addAttribute("matches", matches);

        // A simple "low tutor availability" alert heuristic:
        model.addAttribute("lowTutorAvailability",
                outstandingTutees.size() > outstandingTutors.size() * 2);

        return "admin";
    }
}