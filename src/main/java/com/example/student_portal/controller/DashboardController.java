package com.example.student_portal.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.UserService;

@Controller
public class DashboardController {

    private final UserService userService;
    private final RequestService requestService;
    private final MatchService matchService;

    public DashboardController(UserService userService, RequestService requestService, MatchService matchService) {
        this.userService = userService;
        this.requestService = requestService;
        this.matchService = matchService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

        List<Request> userRequests = requestService.getRequestsByUser(user);
        List<Request> activeRequests = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.OUTSTANDING).collect(Collectors.toList());
        List<Request> matchedRequests = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.MATCHED).collect(Collectors.toList());

        List<Match> userMatches = matchService.findMatchesByUser(user);
        int profileScore = calculateProfileCompleteness(user);

        model.addAttribute("user", user);
        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        model.addAttribute("userMatches", userMatches);
        model.addAttribute("profileScore", profileScore);
        model.addAttribute("profileComplete", profileScore >= 80);
        model.addAttribute("totalRequests", userRequests.size());
        model.addAttribute("pendingRequests", activeRequests.size());
        model.addAttribute("completedMatches", userMatches.size());

        return "dashboard";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "landing";
    }

    private int calculateProfileCompleteness(User user) {
        int score = 0;
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty())
            score += 15;
        if (user.getYearGroup() != null)
            score += 15;
        if (user.getExamBoard() != null)
            score += 10;
        if (user.getSubjects() != null && !user.getSubjects().isEmpty())
            score += 20;
        if (user.getAvailabilitySlots() != null && !user.getAvailabilitySlots().isEmpty())
            score += 25;
        if (user.getMaxSessionsPerWeek() != null)
            score += 15;
        return Math.min(score, 100);
    }
}