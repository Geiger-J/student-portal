package com.example.student_portal.controller;

/*
 * Central dashboard controller for consolidated user experience.
 * Displays overview of requests, matches, profile status, and quick actions.
 * Integrates formerly separate requests and matches functionality.
 * Provides unified interface for main operations.
 */

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.student_portal.dto.RequestForm;
import com.example.student_portal.entity.Match;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;

import jakarta.validation.Valid;

@Controller
@Validated
public class DashboardController {

    private final UserService userService;
    private final RequestService requestService;
    private final MatchService matchService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;

    public DashboardController(UserService userService, RequestService requestService, MatchService matchService, SubjectService subjectService, TimeslotService timeslotService) {
        this.userService = userService;
        this.requestService = requestService;
        this.matchService = matchService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

        // Gather user requests & matches
        List<Request> userRequests = requestService.getRequestsByUser(user);
        List<Request> activeRequests = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.OUTSTANDING).collect(Collectors.toList());
        List<Request> matchedRequests = userRequests.stream().filter(r -> r.getStatus() == RequestStatus.MATCHED).collect(Collectors.toList());

        List<Match> userMatches = matchService.findMatchesByUser(user);

        // Profile completeness
        int profileScore = calculateProfileCompleteness(user);

        // Model attributes for display
        model.addAttribute("user", user);
        model.addAttribute("activeRequests", activeRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        model.addAttribute("userMatches", userMatches);
        model.addAttribute("profileScore", profileScore);
        model.addAttribute("profileComplete", profileScore >= 80);
        model.addAttribute("totalRequests", userRequests.size());
        model.addAttribute("pendingRequests", activeRequests.size());
        model.addAttribute("completedMatches", userMatches.size());

        // Form-backing bean for inline request creation
        RequestForm form = new RequestForm();
        form.setTargetWeek(getUpcomingMondayDate().toString());
        model.addAttribute("requestForm", form);

        // Reference data for form selects / checkboxes
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("upcomingMonday", getUpcomingMondayDate().toString());

        return "dashboard";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails principal) { return principal != null ? "redirect:/dashboard" : "landing"; }

    /**
     * Create a new request from the dashboard inline form.
     */
    @PostMapping("/dashboard/requests/add")
    public String addRequest(@AuthenticationPrincipal UserDetails principal, @ModelAttribute("requestForm") @Valid RequestForm form, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        try {
            Request request = requestService.createRequest(user, form.getSubjectId(), form.getTimeslotIds(), form.getType());

            if (form.getTargetWeek() != null && !form.getTargetWeek().isBlank()) {
                LocalDate targetWeek = LocalDate.parse(form.getTargetWeek());
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
     * Cancel a request from dashboard context.
     */
    @PostMapping("/dashboard/requests/cancel/{id}")
    public String cancelRequest(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
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
     * Mark a matched/completed request as completed (if you have such UI).
     */
    @PostMapping("/dashboard/requests/complete/{id}")
    public String completeRequest(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            model.addAttribute("errorMessage", "Request not found or access denied");
            return "redirect:/dashboard#requests";
        }
        try {
            requestService.updateStatus(id, RequestStatus.COMPLETED);
            model.addAttribute("successMessage", "Request marked as completed!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to complete request: " + e.getMessage());
        }
        return "redirect:/dashboard#requests";
    }

    private LocalDate getUpcomingMondayDate() {
        LocalDate today = LocalDate.now();
        // If today is Monday use it; else find next Monday
        while (today.getDayOfWeek().getValue() != 1) {
            today = today.plusDays(1);
        }
        return today;
    }

    private boolean isRequestOwnedByUser(Request req, UserDetails principal) {
        return req.getUser() != null && principal != null && principal.getUsername().equalsIgnoreCase(req.getUser().getEmail());
    }

    private int calculateProfileCompleteness(User user) {
        int score = 0;
        if (user.getFullName() != null && !user.getFullName().isBlank())
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