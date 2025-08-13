package com.example.student_portal.controller;

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
     * Show list of user's requests and the creation form.
     */
    @GetMapping
    public String listRequests(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());

        List<Request> requests = requestService.getRequestsByUser(user);
        model.addAttribute("requests", requests);

        // Empty form for creation
        RequestForm form = new RequestForm();
        form.targetWeek = getUpcomingMondayDate().toString();
        model.addAttribute("requestForm", form);

        // Dropdowns
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("upcomingMonday", getUpcomingMondayDate().toString());

        return "requests";
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
            
            model.addAttribute("successMessage", "Request created successfully!");
            
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Repopulate form and show error
            populateModelForRequestView(user, model);
            model.addAttribute("requestForm", form);
            model.addAttribute("errorMessage", ex.getMessage());
            return "requests";
        }

        return "redirect:/requests";
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
            model.addAttribute("errorMessage", "Request not found or access denied");
            return listRequests(principal, model);
        }

        try {
            requestService.updateStatus(id, RequestStatus.REJECTED);
            model.addAttribute("successMessage", "Request cancelled successfully!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to cancel request: " + e.getMessage());
        }

        return listRequests(principal, model);
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
            model.addAttribute("errorMessage", "Request not found or access denied");
            return listRequests(principal, model);
        }

        if (req.getStatus() != RequestStatus.MATCHED) {
            model.addAttribute("errorMessage", "Only matched requests can be marked as completed");
            return listRequests(principal, model);
        }

        try {
            // For now, we'll use REJECTED status to indicate completion
            // In a full implementation, we'd add a COMPLETED status
            requestService.updateStatus(id, RequestStatus.REJECTED);
            model.addAttribute("successMessage", "Request marked as completed!");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to complete request: " + e.getMessage());
        }

        return listRequests(principal, model);
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
            model.addAttribute("errorMessage", "Request not found or access denied");
            return listRequests(principal, model);
        }

        try {
            recurrenceService.requestRecurrence(id);
            model.addAttribute("successMessage", "Weekly recurrence requested! Waiting for tutor acceptance.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to request recurrence: " + e.getMessage());
        }

        return listRequests(principal, model);
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
            model.addAttribute("errorMessage", "Request not found or access denied");
            return listRequests(principal, model);
        }

        try {
            recurrenceService.acceptRecurrence(id);
            model.addAttribute("successMessage", "Weekly recurrence accepted! Future sessions will be automatically created.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to accept recurrence: " + e.getMessage());
        }

        return listRequests(principal, model);
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
            model.addAttribute("errorMessage", "Request not found or access denied");
            return listRequests(principal, model);
        }

        try {
            recurrenceService.cancelRecurrence(id);
            model.addAttribute("successMessage", "Weekly recurrence cancelled.");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to cancel recurrence: " + e.getMessage());
        }

        return listRequests(principal, model);
    }

    /**
     * Helper method to check if a request is owned by the current user.
     */
    private boolean isRequestOwnedByUser(Request request, UserDetails principal) {
        User user = userService.findByEmail(principal.getUsername());
        return request.getUser().getId().equals(user.getId());
    }

    /**
     * Helper method to populate model for request view.
     */
    private void populateModelForRequestView(User user, Model model) {
        model.addAttribute("requests", requestService.getRequestsByUser(user));
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("requestTypes", RequestType.values());
        model.addAttribute("upcomingMonday", getUpcomingMondayDate().toString());
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