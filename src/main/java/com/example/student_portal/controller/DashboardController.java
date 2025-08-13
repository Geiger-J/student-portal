package com.example.student_portal.controller;

/*
 * Central dashboard controller for consolidated user experience.
 * Displays overview of requests, matches, profile status, and quick actions.
 * Integrates formerly separate requests and matches functionality.
 * Provides unified interface for student portal main operations.
 */

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.temporal.WeekFields;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.UserService;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;

import jakarta.validation.constraints.NotNull;

@Controller
public class DashboardController {

    private final UserService userService;
    private final RequestService requestService;
    private final MatchService matchService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;

    public DashboardController(UserService userService, RequestService requestService, MatchService matchService,
                             SubjectService subjectService, TimeslotService timeslotService) {
        this.userService = userService;
        this.requestService = requestService;
        this.matchService = matchService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
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

        // Add form data for request creation
        RequestForm form = new RequestForm();
        form.targetWeek = getUpcomingMondayDate().toString();
        model.addAttribute("requestForm", form);
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("upcomingMonday", getUpcomingMondayDate().toString());

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

    /**
     * DTO for request creation from dashboard.
     */
    public static class RequestForm {
        @NotNull public Long subjectId;
        @NotNull public RequestType type;
        @NotNull public List<Long> timeslotIds;
        public String targetWeek; // ISO date string for target Monday
    }

    /**
     * Handle creation of a new request from consolidated dashboard.
     */
    @PostMapping("/dashboard/requests/add")
    public String addRequest(@AuthenticationPrincipal UserDetails principal,
                           @ModelAttribute("requestForm") @Validated RequestForm form,
                           Model model) {
        User user = userService.findByEmail(principal.getUsername());

        try {
            Request request = requestService.createRequest(user, form.subjectId, form.timeslotIds, form.type);
            
            // Set target week if provided
            if (form.targetWeek != null && !form.targetWeek.isEmpty()) {
                LocalDate targetWeek = LocalDate.parse(form.targetWeek);
                request.setTargetWeek(targetWeek);
                requestService.updateRequest(request);
            }
            
            model.addAttribute("successMessage", "Request created successfully!");
            
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Cancel a request from dashboard.
     */
    @PostMapping("/dashboard/requests/cancel/{id}")
    public String cancelRequest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails principal,
                              Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            model.addAttribute("errorMessage", "Request not found or access denied");
            return "redirect:/dashboard#requests";
        }

        try {
            requestService.updateStatus(id, RequestStatus.REJECTED);
            model.addAttribute("successMessage", "Request cancelled successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to cancel request: " + e.getMessage());
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Helper method to check if a request is owned by the current user.
     */
    private boolean isRequestOwnedByUser(Request request, UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername());
        return request.getUser().getId().equals(user.getId());
    }

    /**
     * Get the upcoming Monday date (ISO week format).
     */
    private LocalDate getUpcomingMondayDate() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.ISO;
        return today.with(weekFields.dayOfWeek(), 1).plusWeeks(1); // Next Monday
    }
}