package com.example.student_portal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.student_portal.dto.RegistrationDto;
import com.example.student_portal.service.UserService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("form") RegistrationDto form, BindingResult errors, Model model) {
        log.debug("POST /register attempt email={}", form.getEmail());

        if (userService.existsByEmail(form.getEmail())) {
            errors.rejectValue("email", "exists", "Email already in use");
        }

        if (errors.hasErrors()) {
            log.debug("Registration validation errors: {}", errors);
            return "auth/register";
        }

        // Register with null yearGroup and examBoard (set later on profile)
        userService.registerUser(form.getFullName(), form.getEmail(), form.getPassword(), null, null);

        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.info("Registered and auto-logged in user {}", form.getEmail());
        return "redirect:/dashboard";
    }
}