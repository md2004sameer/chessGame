package com.example.ChessGame.controller;

// Avoid direct dependency on Spring Security types so this advice compiles even when
// Spring Security is not on the classpath.
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("_csrf")
    public Object csrfToken(HttpServletRequest request) {
        // returns null if not present; Thymeleaf guarded checks will avoid NPE
        return request.getAttribute("org.springframework.security.web.csrf.CsrfToken");
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.thymeleaf.exceptions.TemplateProcessingException.class)
    public String handleTemplateError(org.thymeleaf.exceptions.TemplateProcessingException ex, jakarta.servlet.http.HttpServletRequest request, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        org.slf4j.LoggerFactory.getLogger(GlobalControllerAdvice.class).error("Template processing error for request {}", request.getRequestURI(), ex);
        redirectAttributes.addFlashAttribute("error", "An unexpected error occurred while rendering the page: " + ex.getMessage());
        return "redirect:/";
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, jakarta.servlet.http.HttpServletRequest request, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        org.slf4j.LoggerFactory.getLogger(GlobalControllerAdvice.class).error("Unhandled exception for request {}", request.getRequestURI(), ex);
        redirectAttributes.addFlashAttribute("error", "An unexpected server error occurred: " + ex.getMessage());
        return "redirect:/";
    }
}
