package com.example.student_portal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.student_portal.entity.User;
import com.example.student_portal.model.Role;
import com.example.student_portal.repository.UserRepository;

/**
 * Unit test for UserService registration with optional yearGroup and examBoard.
 */
class UserServiceRegistrationTest {

    private UserService userService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void testRegistrationWithNullYearGroupAndExamBoard() {
        // Mock behavior
        when(userRepository.existsByEmail("12345678@bromsgrove-school.co.uk")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Test registration with null yearGroup and examBoard
        User result = userService.registerUser(
            "Test User", 
            "12345678@bromsgrove-school.co.uk", 
            "password123", 
            null,  // yearGroup is null
            null   // examBoard is null
        );
        
        // Assertions
        assertNotNull(result);
        assertEquals("Test User", result.getFullName());
        assertEquals("12345678@bromsgrove-school.co.uk", result.getEmail());
        assertEquals("hashedPassword", result.getPasswordHash());
        assertEquals(Role.STUDENT, result.getRole());
        assertNull(result.getYearGroup()); // Should be null
        assertNull(result.getExamBoard()); // Should be null
        
        // Verify repository interactions
        verify(userRepository).existsByEmail("12345678@bromsgrove-school.co.uk");
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }
    
    @Test
    void testRoleInferenceFromEmail() {
        // Test student email (starts with digit)
        assertEquals(Role.STUDENT, userService.inferRoleFromEmail("12345678@bromsgrove-school.co.uk"));
        
        // Test admin email (starts with letter)
        assertEquals(Role.ADMIN, userService.inferRoleFromEmail("admin@bromsgrove-school.co.uk"));
        assertEquals(Role.ADMIN, userService.inferRoleFromEmail("teacher.name@bromsgrove-school.co.uk"));
    }
}