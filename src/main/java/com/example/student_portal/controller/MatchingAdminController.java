package com.example.student_portal.controller;

import com.example.student_portal.service.MatchingService;
import com.example.student_portal.service.RecurrenceService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.MatchService;
import com.example.student_portal.util.MatchingAlgorithm;
import com.example.student_portal.model.RequestStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Admin controller for managing the matching algorithm and viewing results.
 * 
 * Restricted to ADMIN role only.
 * Provides:
 * - Manual trigger of matching algorithm
 * - View last run summary and statistics
 * - Management of recurrence settings
 */
@Controller
@RequestMapping("/admin/matching")
@PreAuthorize("hasRole('ADMIN')")
public class MatchingAdminController {
    
    private final MatchingAlgorithm matchingAlgorithm;
    private final MatchingService matchingService;
    private final RecurrenceService recurrenceService;
    private final RequestService requestService;
    private final MatchService matchService;
    
    public MatchingAdminController(MatchingAlgorithm matchingAlgorithm,
                                 MatchingService matchingService,
                                 RecurrenceService recurrenceService,
                                 RequestService requestService,
                                 MatchService matchService) {
        this.matchingAlgorithm = matchingAlgorithm;
        this.matchingService = matchingService;
        this.recurrenceService = recurrenceService;
        this.requestService = requestService;
        this.matchService = matchService;
    }
    
    /**
     * Main matching admin dashboard.
     */
    @GetMapping
    public String matchingDashboard(Model model) {
        // Get current statistics
        long outstandingTutors = requestService.getOutstandingTutorRequests().size();
        long outstandingTutees = requestService.getOutstandingTuteeRequests().size();
        long totalMatches = matchService.findAllMatches().size();
        long totalRequests = requestService.getAllRequests().size();
        
        model.addAttribute("outstandingTutors", outstandingTutors);
        model.addAttribute("outstandingTutees", outstandingTutees);
        model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("totalRequests", totalRequests);
        
        // Calculate matching potential and efficiency
        long matchingPotential = Math.min(outstandingTutors, outstandingTutees);
        double efficiency = totalRequests > 0 ? (double) totalMatches / totalRequests * 100 : 0;
        
        model.addAttribute("matchingPotential", matchingPotential);
        model.addAttribute("efficiency", String.format("%.1f", efficiency));
        
        return "admin/matching/dashboard";
    }
    
    /**
     * Manually trigger the matching algorithm.
     */
    @PostMapping("/run")
    public String runMatching(Model model) {
        try {
            matchingAlgorithm.runMatching();
            model.addAttribute("successMessage", "Matching algorithm completed successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Matching algorithm failed: " + e.getMessage());
        }
        
        return matchingDashboard(model);
    }
    
    /**
     * Run matching for a specific target week.
     */
    @PostMapping("/run-for-week")
    public String runMatchingForWeek(@RequestParam("targetWeek") String targetWeekStr, Model model) {
        try {
            LocalDate targetWeek = LocalDate.parse(targetWeekStr);
            int matchesCreated = matchingService.performMatchingForWeek(targetWeek);
            model.addAttribute("successMessage", 
                String.format("Matching completed for week %s: %d matches created!", 
                             targetWeekStr, matchesCreated));
        } catch (Exception e) {
            model.addAttribute("errorMessage", 
                "Matching failed for specified week: " + e.getMessage());
        }
        
        return matchingDashboard(model);
    }
    
    /**
     * Generate recurring requests manually.
     */
    @PostMapping("/generate-recurring")
    public String generateRecurringRequests(Model model) {
        try {
            recurrenceService.generateRecurringRequests();
            model.addAttribute("successMessage", "Recurring requests generated successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to generate recurring requests: " + e.getMessage());
        }
        
        return matchingDashboard(model);
    }
    
    /**
     * Clear all matches (for testing purposes).
     */
    @PostMapping("/clear-matches")
    public String clearAllMatches(Model model) {
        try {
            matchService.clearAllMatches();
            
            // Reset all matched requests to outstanding
            requestService.getAllRequests().forEach(request -> {
                if (request.getStatus() == RequestStatus.MATCHED) {
                    requestService.updateStatus(request.getId(), RequestStatus.OUTSTANDING);
                }
            });
            
            model.addAttribute("successMessage", "All matches cleared and requests reset!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to clear matches: " + e.getMessage());
        }
        
        return matchingDashboard(model);
    }
    
    /**
     * View detailed matching statistics and history.
     */
    @GetMapping("/stats")
    public String matchingStats(Model model) {
        // Add detailed statistics for analysis
        model.addAttribute("allMatches", matchService.findAllMatches());
        model.addAttribute("allRequests", requestService.getAllRequests());
        
        // Group statistics by status
        long outstandingRequests = requestService.getAllRequests().stream()
            .mapToLong(r -> r.getStatus() == RequestStatus.OUTSTANDING ? 1 : 0)
            .sum();
        long matchedRequests = requestService.getAllRequests().stream()
            .mapToLong(r -> r.getStatus() == RequestStatus.MATCHED ? 1 : 0)
            .sum();
        
        model.addAttribute("outstandingRequests", outstandingRequests);
        model.addAttribute("matchedRequests", matchedRequests);
        
        return "admin/matching/stats";
    }
    
    /**
     * View and manage recurrence settings.
     */
    @GetMapping("/recurrence")
    public String recurrenceManagement(Model model) {
        // Get all recurring requests
        var recurringRequests = requestService.getAllRequests().stream()
            .filter(r -> r.isRecurring())
            .toList();
        
        model.addAttribute("recurringRequests", recurringRequests);
        model.addAttribute("nextWeekDate", 
            LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        return "admin/matching/recurrence";
    }
}