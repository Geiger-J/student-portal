package com.example.student_portal.controller;

import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.model.RequestStatus;
import com.example.student_portal.model.RequestType;
import com.example.student_portal.service.RequestService;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.TimeslotService;
import com.example.student_portal.service.UserService;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller to manage tutor/tutee requests.
 * Lists user requests and handles creation/deletion.
 */
@Controller
@RequestMapping("/requests")
@Validated
public class RequestController {

    private final RequestService requestService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final TimeslotService timeslotService;

    public RequestController(RequestService requestService,
                             UserService userService,
                             SubjectService subjectService,
                             TimeslotService timeslotService) {
        this.requestService = requestService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.timeslotService = timeslotService;
    }

    /**
     * Simple DTO to receive request creation input from the form.
     */
    public static class RequestForm {
        @NotNull public Long subjectId;
        @NotNull public RequestType type;
        @NotNull public List<Long> timeslotIds;
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
        model.addAttribute("requestForm", new RequestForm());

        // Dropdowns
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("timeslots", timeslotService.findAll());
        model.addAttribute("requestTypes", RequestType.values());

        return "requests";
    }

    /**
     * Handle creation of a new request.
     */
    @PostMapping("/add")
    public String addRequest(@AuthenticationPrincipal UserDetails principal,
                             @ModelAttribute("requestForm") @Validated RequestForm form,
                             Model model) {
        User user = userService.findByEmail(principal.getUsername());

        try {
            requestService.createRequest(user, form.subjectId, form.timeslotIds, form.type);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Repopulate form and show error
            model.addAttribute("requests", requestService.getRequestsByUser(user));
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("timeslots", timeslotService.findAll());
            model.addAttribute("requestTypes", RequestType.values());
            model.addAttribute("requestForm", form);
            model.addAttribute("error", ex.getMessage());
            return "requests";
        }

        return "redirect:/requests";
    }

    /**
     * Delete a request owned by the user.
     */
    @PostMapping("/delete/{id}")
    public String deleteRequest(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal) {
        Request req = requestService.findById(id);
        if (req == null) return "redirect:/requests?error=notfound";

        User user = userService.findByEmail(principal.getUsername());
        if (!req.getUser().getId().equals(user.getId())) {
            return "redirect:/requests?error=forbidden";
        }

        // Only allow deletion if still outstanding
        if (req.getStatus() == RequestStatus.OUTSTANDING) {
            requestService.deleteRequest(id);
        }

        return "redirect:/requests";
    }
}