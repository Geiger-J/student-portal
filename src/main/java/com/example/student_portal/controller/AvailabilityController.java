package com.example.student_portal.controller;

/*
 * Controller for user availability slot management with consolidation support.
 * Manages interactive grid interface for Monday-Friday, periods 1-7 scheduling.
 * Provides redirect from legacy /availability route to consolidated profile page.
 * Maintains backwards compatibility while encouraging profile page usage.
 */

import com.example.student_portal.entity.User;
import com.example.student_portal.model.Period;
import com.example.student_portal.service.AvailabilityService;
import com.example.student_portal.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Controller for managing user availability slots.
 * 
 * Provides an interactive grid interface for Monday–Friday, periods 1–7
 * where users can add/remove their availability for tutoring sessions.
 */
@Controller
@RequestMapping("/availability")
public class AvailabilityController {
    
    private final AvailabilityService availabilityService;
    private final UserService userService;
    
    public AvailabilityController(AvailabilityService availabilityService, UserService userService) {
        this.availabilityService = availabilityService;
        this.userService = userService;
    }
    
    /**
     * Redirect legacy availability page to consolidated profile.
     * Maintains backwards compatibility for bookmarked URLs.
     */
    @GetMapping
    public String redirectToProfile() {
        return "redirect:/profile#availability";
    }
    
    /**
     * Add an availability slot for the current user.
     */
    @PostMapping("/add")
    public String addAvailability(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam("dayOfWeek") String dayOfWeek,
                                 @RequestParam("period") String period,
                                 Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            Period per = Period.valueOf(period.toUpperCase());
            
            availabilityService.addAvailabilitySlot(user, day, per);
            
        } catch (IllegalArgumentException e) {
            return "redirect:/profile#availability?error=invalid";
        } catch (Exception e) {
            return "redirect:/profile#availability?error=add";
        }
        
        return "redirect:/profile#availability";
    }
    
    /**
     * Remove an availability slot for the current user.
     */
    @PostMapping("/remove")
    public String removeAvailability(@AuthenticationPrincipal UserDetails principal,
                                   @RequestParam("dayOfWeek") String dayOfWeek,
                                   @RequestParam("period") String period,
                                   Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            DayOfWeek day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
            Period per = Period.valueOf(period.toUpperCase());
            
            availabilityService.removeAvailabilitySlot(user, day, per);
            
        } catch (IllegalArgumentException e) {
            return "redirect:/profile#availability?error=invalid";
        } catch (Exception e) {
            return "redirect:/profile#availability?error=remove";
        }
        
        return "redirect:/profile#availability";
    }
    
    /**
     * Clear all availability slots for the current user.
     */
    @PostMapping("/clear")
    public String clearAvailability(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            availabilityService.clearAllAvailability(user);
            
        } catch (Exception e) {
            return "redirect:/profile#availability?error=clear";
        }
        
        return "redirect:/profile#availability";
    }
    
    /**
     * Bulk update availability slots via form submission.
     * Expects checkbox inputs with names like "slot_MONDAY_P1", "slot_TUESDAY_P3", etc.
     */
    @PostMapping("/bulk-update")
    public String bulkUpdateAvailability(@AuthenticationPrincipal UserDetails principal,
                                       @RequestParam(required = false) List<String> slots,
                                       Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            // Clear existing availability
            availabilityService.clearAllAvailability(user);
            
            // Add selected slots
            if (slots != null) {
                for (String slotKey : slots) {
                    String[] parts = slotKey.split("_");
                    if (parts.length == 2) {
                        DayOfWeek day = DayOfWeek.valueOf(parts[0].toUpperCase());
                        Period period = Period.valueOf(parts[1].toUpperCase());
                        availabilityService.addAvailabilitySlot(user, day, period);
                    }
                }
            }
            
        } catch (Exception e) {
            return "redirect:/profile#availability?error=update";
        }
        
        return "redirect:/profile#availability";
    }
}