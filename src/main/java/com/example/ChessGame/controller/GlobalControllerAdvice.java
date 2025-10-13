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
}
