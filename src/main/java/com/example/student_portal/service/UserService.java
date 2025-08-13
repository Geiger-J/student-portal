package com.example.student_portal.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.student_portal.entity.User;
import com.example.student_portal.model.ExamBoard;
import com.example.student_portal.model.Role;
import com.example.student_portal.model.YearGroup;
import com.example.student_portal.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // interface

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String fullName, String email, String rawPassword, YearGroup yearGroup, ExamBoard examBoard) {
        String normalizedEmail = email.toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        Role role = inferRoleFromEmail(normalizedEmail);

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setYearGroup(yearGroup);
        user.setExamBoard(examBoard);

        return userRepository.save(user);
    }

    public Role inferRoleFromEmail(String email) {
        String prefix = email.split("@")[0];
        if (prefix.isEmpty())
            return Role.STUDENT;
        char firstChar = prefix.charAt(0);
        return Character.isDigit(firstChar) ? Role.STUDENT : Role.ADMIN;
    }

    public User save(User user) { return userRepository.save(user); }

    public User findByEmail(String email) { return userRepository.findByEmail(email.toLowerCase()).orElse(null); }

    public List<User> findAllUsers() { return userRepository.findAll(); }

    public boolean existsByEmail(String email) { return userRepository.existsByEmail(email.toLowerCase()); }
}