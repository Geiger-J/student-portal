package com.example.student_portal.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.student_portal.entity.User;
import com.example.student_portal.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) { this.userRepository = userRepository; }

    // "username" is the email from the login form
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(username.toLowerCase()).orElseThrow(() -> new UsernameNotFoundException("No user with email " + username));

        return org.springframework.security.core.userdetails.User.withUsername(u.getEmail()).password(u.getPasswordHash()).roles(u.getRole().name()) // becomes ROLE_STUDENT / ROLE_ADMIN
                .accountExpired(false).accountLocked(false).credentialsExpired(false).disabled(false).build();
    }
}