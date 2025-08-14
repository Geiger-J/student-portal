package com.example.student_portal.controller;

/*
 * Controller for tutoring request management with consolidation support.
 * Handles request creation, status management, and recurrence functionality.
 * Provides redirect from legacy /requests route to consolidated dashboard.
 */

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.student_portal.dto.RequestForm;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.service.RecurrenceService;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/requests")
@Validated
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;
    private final RecurrenceService recurrenceService;

    public RequestController(RequestService requestService, UserService userService, SubjectService subjectService, TimeslotService timeslotService, RecurrenceService recurrenceService) {
        this.requestService = requestService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
        this.recurrenceService = recurrenceService;
    }

    @GetMapping
    public String redirectToDashboard() { return "redirect:/dashboard#requests"; }

    /**
     * Handle creation of a new request (legacy /requests path).
     */
    @PostMapping("/add")
    public String addRequest(@AuthenticationPrincipal UserDetails principal, @ModelAttribute("requestForm") @Valid RequestForm form, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        try {
            Request request = requestService.createRequest(user, form.getSubjectId(), form.getTimeslotIds(), form.getType());

            if (form.getTargetWeek() != null && !form.getTargetWeek().isBlank()) {
                LocalDate targetWeek = LocalDate.parse(form.getTargetWeek());
                request.setTargetWeek(targetWeek);
                requestService.updateRequest(request);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "redirect:/dashboard#requests?error=create";
        }
        return "redirect:/dashboard#requests";
    }

    @PostMapping("/cancel/{id}")
    public String cancelRequest(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
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

    @PostMapping("/complete/{id}")
    public String completeRequest(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        Request req = requestService.findById(id);
        if (req == null || !isRequestOwnedByUser(req, principal)) {
            return "redirect:/dashboard#requests?error=notfound";
        }
        try {
            requestService.updateStatus(id, RequestStatus.COMPLETED);
        } catch (Exception e) {
            return "redirect:/dashboard#requests?error=complete";
        }
        return "redirect:/dashboard#requests";
    }

    private boolean isRequestOwnedByUser(Request req, UserDetails principal) {
        return req.getUser() != null && principal != null && principal.getUsername().equalsIgnoreCase(req.getUser().getEmail());
    }
}