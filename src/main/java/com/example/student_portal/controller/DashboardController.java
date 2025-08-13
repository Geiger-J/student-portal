package com.example.student_portal.controller;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard controller for the student homepage.
 * 
 * Shows profile completeness, active & pending requests, matched partners,
 * and provides navigation to other features.
 */
@Controller
public class DashboardController {
    
    private final UserService userService;
    private final RequestService requestService;
    private final MatchService matchService;
    
    public DashboardController(UserService userService, 
                             RequestService requestService,
                             MatchService matchService) {
        this.userService = userService;
        this.requestService = requestService;
        this.matchService = matchService;
    }
    
    /**
     * Student dashboard showing overview of current status.
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        // Get user's requests
        List<Request> userRequests = requestService.getRequestsByUser(user);
        List<Request> activeRequests = userRequests.stream()
            .filter(r -> r.getStatus() == RequestStatus.OUTSTANDING)
            .collect(Collectors.toList());
        List<Request> matchedRequests = userRequests.stream()
            .filter(r -> r.getStatus() == RequestStatus.MATCHED)
            .collect(Collectors.toList());
        
        // Get user's matches
        List<Match> userMatches = matchService.findMatchesByUser(user);
        
        // Calculate profile completeness
        int profileScore = calculateProfileCompleteness(user);
        
        // Add model attributes
        model.addAttribute("user", user);
        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        model.addAttribute("userMatches", userMatches);
        model.addAttribute("profileScore", profileScore);
        model.addAttribute("profileComplete", profileScore >= 80);
        
        // Add quick stats
        model.addAttribute("totalRequests", userRequests.size());
        model.addAttribute("pendingRequests", activeRequests.size());
        model.addAttribute("completedMatches", userMatches.size());
        
        return "dashboard";
    }
    
    /**
     * Alternative root mapping to dashboard for logged-in users.
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails principal) {
        if (principal != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
    
    /**
     * Calculate profile completeness percentage.
     * Used to encourage users to complete their profiles.
     */
    private int calculateProfileCompleteness(User user) {
        int score = 0;
        int maxScore = 100;
        
        // Basic profile fields (40 points)
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            score += 15;
        }
        if (user.getYearGroup() != null) {
            score += 15;
        }
        if (user.getExamBoard() != null) {
            score += 10;
        }
        
        // Subjects (20 points)
        if (user.getSubjects() != null && !user.getSubjects().isEmpty()) {
            score += 20;
        }
        
        // Availability (25 points)
        if (user.getAvailabilitySlots() != null && !user.getAvailabilitySlots().isEmpty()) {
            score += 25;
        }
        
        // Max sessions per week set (15 points)
        if (user.getMaxSessionsPerWeek() != null && user.getMaxSessionsPerWeek() > 0) {
            score += 15;
        }
        
        return Math.min(score, maxScore);
    }
}