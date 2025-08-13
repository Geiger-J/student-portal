package com.example.student_portal.controller;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.User;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller to display the logged-in user's matches (as tutor or tutee).
 */
@Controller
public class MatchController {

    private final MatchService matchService;
    private final UserService userService;

    public MatchController(MatchService matchService,
                           UserService userService) {
        this.matchService = matchService;
        this.userService = userService;
    }

    @GetMapping("/matches")
    public String showMatches(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        List<Match> matches = matchService.findMatchesByUser(user);
        model.addAttribute("matches", matches);
        return "matches";
    }
}