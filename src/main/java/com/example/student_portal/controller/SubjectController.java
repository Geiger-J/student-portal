package com.example.student_portal.controller;

import com.example.student_portal.entity.Subject;
import com.example.student_portal.entity.User;
import com.example.student_portal.service.SubjectService;
import com.example.student_portal.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for managing subjects.
 * 
 * Provides:
 * - Student interface for selecting subjects they study or can tutor
 * - Admin interface for managing the master list of subjects
 */
@Controller
@RequestMapping("/subjects")
public class SubjectController {
    
    private final SubjectService subjectService;
    private final UserService userService;
    
    public SubjectController(SubjectService subjectService, UserService userService) {
        this.subjectService = subjectService;
        this.userService = userService;
    }
    
    /**
     * Student view: Select subjects they study or can tutor.
     */
    @GetMapping
    public String manageSubjects(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername());
        List<Subject> allSubjects = subjectService.findAll();
        Set<Long> userSubjectIds = user.getSubjects().stream()
            .map(Subject::getId)
            .collect(Collectors.toSet());
        
        model.addAttribute("user", user);
        model.addAttribute("allSubjects", allSubjects);
        model.addAttribute("userSubjectIds", userSubjectIds);
        
        return "subjects/manage";
    }
    
    /**
     * Update user's subject selections.
     */
    @PostMapping("/update")
    public String updateUserSubjects(@AuthenticationPrincipal UserDetails principal,
                                   @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                                   Model model) {
        User user = userService.findByEmail(principal.getUsername());
        
        try {
            if (subjectIds != null && !subjectIds.isEmpty()) {
                Set<Subject> selectedSubjects = subjectIds.stream()
                    .map(id -> subjectService.findById(id))
                    .filter(subject -> subject != null)
                    .collect(Collectors.toSet());
                
                user.setSubjects(selectedSubjects);
            } else {
                user.getSubjects().clear();
            }
            
            userService.save(user);
            model.addAttribute("successMessage", "Subjects updated successfully!");
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to update subjects: " + e.getMessage());
        }
        
        return manageSubjects(principal, model);
    }
    
    /**
     * Admin view: Manage master list of subjects.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminSubjects(Model model) {
        List<Subject> allSubjects = subjectService.findAll();
        model.addAttribute("subjects", allSubjects);
        model.addAttribute("newSubject", new Subject());
        
        return "subjects/admin";
    }
    
    /**
     * Admin: Add a new subject.
     */
    @PostMapping("/admin/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addSubject(@ModelAttribute Subject subject, Model model) {
        try {
            if (subject.getName() == null || subject.getName().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Subject name cannot be empty");
            } else {
                subjectService.save(subject);
                model.addAttribute("successMessage", "Subject added successfully!");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to add subject: " + e.getMessage());
        }
        
        return adminSubjects(model);
    }
    
    /**
     * Admin: Delete a subject.
     */
    @PostMapping("/admin/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSubject(@PathVariable Long id, Model model) {
        try {
            Subject subject = subjectService.findById(id);
            if (subject != null) {
                // Check if subject is in use
                if (subjectService.isSubjectInUse(id)) {
                    model.addAttribute("errorMessage", 
                        "Cannot delete subject - it is currently being used by users or requests");
                } else {
                    subjectService.deleteById(id);
                    model.addAttribute("successMessage", "Subject deleted successfully!");
                }
            } else {
                model.addAttribute("errorMessage", "Subject not found");
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to delete subject: " + e.getMessage());
        }
        
        return adminSubjects(model);
    }
}