package com.example.student_portal.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.student_portal.dto.RegistrationDto;
import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegistrationDto());
        model.addAttribute("yearGroups", YearGroup.values());
        model.addAttribute("examBoards", ExamBoard.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid RegistrationDto form, BindingResult errors, Model model) {
        if (userService.existsByEmail(form.getEmail())) {
            errors.rejectValue("email", "exists", "Email already in use");
        }
        if (errors.hasErrors()) {
            model.addAttribute("form", form);
            model.addAttribute("yearGroups", YearGroup.values());
            model.addAttribute("examBoards", ExamBoard.values());
            return "auth/register";
        }

        userService.registerUser(form.getFullName(), form.getEmail(), form.getPassword(), form.getYearGroup(), form.getExamBoard());

        // Auto-login
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        return "redirect:/";
    }
}