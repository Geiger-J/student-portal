package com.example.student_portal.controller;

/*
 * Controller for tutoring request management with consolidation support.
 * Handles request creation, status management, and recurrence functionality.
 * Provides redirect from legacy /requests route to consolidated dashboard.
 * Maintains backwards compatibility while encouraging dashboard usage.
 */

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;
import com.example.student_portal.service.RecurrenceService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;

/**
 * Enhanced controller to manage tutor/tutee requests with new functionality:
 * - Target week selection
 * - Recurrence management (request, accept, cancel)
 * - Status management (cancel, mark completed)
 * - Comprehensive request listing and management
 */
@Controller
@RequestMapping("/requests")
@Validated
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;
    private final RecurrenceService recurrenceService;

    public RequestController(RequestService requestService,
                           UserService userService,
                           SubjectService subjectService,
                           TimeslotService timeslotService,
                           RecurrenceService recurrenceService) {
        this.requestService = requestService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
        this.recurrenceService = recurrenceService;
    }

    /**
     * Enhanced DTO for request creation with target week support.
     */
    public static class RequestForm {
        @NotNull public Long subjectId;
        @NotNull public RequestType type;
        @NotNull public List<Long> timeslotIds;
        public String targetWeek; // ISO date string for target Monday
    }

    /**
     * Redirect legacy requests page to consolidated dashboard.
     * Maintains backwards compatibility for bookmarked URLs.
     */
    @GetMapping
    public String redirectToDashboard() {
        return "redirect:/dashboard#requests";
    }

    /**
     * Handle creation of a new request with enhanced features.
     */
    @PostMapping("/add")
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
            
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "redirect:/dashboard#requests?error=create";
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Cancel a request owned by the user.
     */
    @PostMapping("/cancel/{id}")
    public String cancelRequest(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails principal,
                              Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }

        try {
            requestService.updateStatus(id, RequestStatus.REJECTED);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=cancel";
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Mark a matched request as completed.
     */
    @PostMapping("/complete/{id}")
    public String completeRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal,
                                Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }

        if (req.getStatus() != RequestStatus.MATCHED) {
            return "redirect:/dashboard#requests?error=notmatched";
        }

        try {
            // For now, we'll use REJECTED status to indicate completion
            // In a full implementation, we'd add a COMPLETED status
            requestService.updateStatus(id, RequestStatus.REJECTED);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=complete";
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Request weekly recurrence for a tutee request.
     */
    @PostMapping("/request-recurrence/{id}")
    public String requestRecurrence(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails principal,
                                  Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }

        try {
            recurrenceService.requestRecurrence(id);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=recurrence";
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Accept weekly recurrence for a tutor request.
     */
    @PostMapping("/accept-recurrence/{id}")
    public String acceptRecurrence(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails principal,
                                 Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }

        try {
            recurrenceService.acceptRecurrence(id);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=acceptrecur";
        }

        return "redirect:/dashboard#requests";
    }

    /**
     * Cancel weekly recurrence for a request.
     */
    @PostMapping("/cancel-recurrence/{id}")
    public String cancelRecurrence(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails principal,
                                 Model model) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }

        try {
            recurrenceService.cancelRecurrence(id);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=cancelrecur";
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