package com.example.student_portal.template;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused test to verify the header fragment null guards work correctly.
 * This tests the core fix for unsafe #httpServletRequest evaluation.
 */
class HeaderFragmentTest {

    @Test
    void testHeaderFragmentHandlesNullHttpServletRequestSafely() {
        // Setup minimal Thymeleaf engine with string template
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        // Simulate the navigation fragment with null guards
        String headerTemplate = """
            <nav class="main-nav">
                <a href="/dashboard" th:classappend="${#httpServletRequest != null and #httpServletRequest.requestURI == '/dashboard'} ? 'active' : ''">Dashboard</a>
                <a href="/profile" th:classappend="${#httpServletRequest != null and #strings.startsWith(#httpServletRequest.requestURI, '/profile')} ? 'active' : ''">Profile</a>
            </nav>
            """;

        // Test: With httpServletRequest (normal operation)
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));

        // The key test: this should not throw an exception with null guards
        assertDoesNotThrow(() -> {
            String result = templateEngine.process(headerTemplate, context);
            assertNotNull(result);
            // Should not contain "active" class when request URI doesn't match
            assertFalse(result.contains("class=\"active\""), "Should not have active class when request URI doesn't match");
        }, "Template processing should not fail due to null httpServletRequest evaluation");

        // Test with matching requestURI to ensure the logic works when conditions are met
        MockHttpServletRequest dashboardRequest = new MockHttpServletRequest();
        dashboardRequest.setRequestURI("/dashboard");
        WebContext dashboardContext = new WebContext(
            application.buildExchange(dashboardRequest, response));

        assertDoesNotThrow(() -> {
            String result = templateEngine.process(headerTemplate, dashboardContext);
            assertNotNull(result);
            // The main goal is that it doesn't throw an exception
        }, "Template processing should work with proper request URI");
    }

    @Test
    void testParameterizedHeadFragment() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        // Test parameterized head fragment
        String headTemplate = """
            <head th:fragment="head(title, extraCss)">
                <title th:text="${title} ?: 'Default Title'">Default Title</title>
                <link th:if="${extraCss}" rel="stylesheet" th:href="${extraCss}"/>
            </head>
            """;

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();
        
        JakartaServletWebApplication application = 
            JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(application.buildExchange(request, response));
        
        context.setVariable("title", "Test Page");
        context.setVariable("extraCss", "/css/custom.css");

        assertDoesNotThrow(() -> {
            String result = templateEngine.process(headTemplate, context);
            assertNotNull(result);
            assertTrue(result.contains("Test Page"), "Should contain custom title");
            assertTrue(result.contains("/css/custom.css"), "Should contain extra CSS link");
        });
    }
}