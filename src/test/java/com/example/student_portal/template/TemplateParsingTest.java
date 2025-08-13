package com.example.student_portal.template;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that Thymeleaf templates parse without errors,
 * particularly the header fragments with null guards.
 */
class TemplateParsingTest {

    @Test
    void testHeaderFragmentParses() {
        // Setup Thymeleaf engine
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        // Create web context for proper link resolution
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));
        
        context.setVariable("title", "Test Page");
        context.setVariable("extraCss", null);

        // Test that header fragment can be parsed without httpServletRequest errors
        assertDoesNotThrow(() -> {
            String result = templateEngine.process("fragments/header", context);
            assertNotNull(result);
            assertTrue(result.contains("Test Page"));
        });
    }

    @Test
    void testDashboardTemplateParses() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));
        
        // Add required variables that dashboard.html expects
        context.setVariable("user", createMockUser());
        context.setVariable("profileScore", 85);
        context.setVariable("pendingRequests", 2);
        context.setVariable("completedMatches", 1);
        context.setVariable("totalRequests", 3);
        context.setVariable("activeRequests", java.util.Collections.emptyList());
        context.setVariable("userMatches", java.util.Collections.emptyList());
        context.setVariable("requestTypes", new String[]{"TUTORING", "TUTEE"});
        context.setVariable("subjects", java.util.Collections.emptyList());
        context.setVariable("timeslots", java.util.Collections.emptyList());
        context.setVariable("requestForm", new Object());

        // Test that dashboard can be parsed
        assertDoesNotThrow(() -> {
            String result = templateEngine.process("dashboard", context);
            assertNotNull(result);
            assertTrue(result.contains("Dashboard - Student Portal"));
        });
    }

    @Test
    void testProfileTemplateParses() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));
        
        context.setVariable("user", createMockUser());
        context.setVariable("yearGroups", new String[]{"YEAR_9", "YEAR_10", "YEAR_11", "YEAR_12", "YEAR_13"});
        context.setVariable("examBoards", new String[]{"GCSE", "IB", "A_LEVELS"});
        context.setVariable("periods", java.util.Collections.emptyList());
        context.setVariable("weekdays", java.util.Collections.emptyList());
        context.setVariable("availabilitySlots", java.util.Collections.emptyList());

        // Test that profile can be parsed
        assertDoesNotThrow(() -> {
            String result = templateEngine.process("profile", context);
            assertNotNull(result);
            assertTrue(result.contains("Profile - Student Portal"));
        });
    }

    @Test
    void testNavigationActiveStatesWithNullGuards() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        // Test with null httpServletRequest (fragment parsing scenario)
        MockHttpServletRequest request = null;
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));
        
        context.setVariable("title", "Test Page");
        context.setVariable("extraCss", null);

        // Test that header fragment handles null httpServletRequest gracefully
        assertDoesNotThrow(() -> {
            String result = templateEngine.process("fragments/header", context);
            assertNotNull(result);
            // Should not have active classes when request is null
            assertFalse(result.contains("active"));
        });
    }

    private Object createMockUser() {
        return new Object() {
            public String getFullName() { return "Test User"; }
            public String getYearGroup() { return null; }
            public String getExamBoard() { return null; }
        };
    }
}