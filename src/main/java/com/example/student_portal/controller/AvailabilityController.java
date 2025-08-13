package com.example.student_portal.controller;

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
     * Display the availability grid for the current user.
     */
    @GetMapping
    public String showAvailability(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        model.addAttribute("user", user);
        model.addAttribute("availabilitySlots", availabilityService.getAvailabilitySlots(user));
        model.addAttribute("weekdays", DayOfWeek.values());
        model.addAttribute("periods", Period.values());
        
        return "availability";
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
            model.addAttribute("successMessage", "Availability slot added successfully!");
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid day or period specified");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to add availability slot: " + e.getMessage());
        }
        
        return showAvailability(principal, model);
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
            model.addAttribute("successMessage", "Availability slot removed successfully!");
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Invalid day or period specified");
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to remove availability slot: " + e.getMessage());
        }
        
        return showAvailability(principal, model);
    }
    
    /**
     * Clear all availability slots for the current user.
     */
    @PostMapping("/clear")
    public String clearAvailability(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            availabilityService.clearAllAvailability(user);
            model.addAttribute("successMessage", "All availability slots cleared!");
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to clear availability: " + e.getMessage());
        }
        
        return showAvailability(principal, model);
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
            
            model.addAttribute("successMessage", "Availability updated successfully!");
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to update availability: " + e.getMessage());
        }
        
        return showAvailability(principal, model);
    }
}