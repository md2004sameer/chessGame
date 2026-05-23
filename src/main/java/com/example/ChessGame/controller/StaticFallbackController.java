package com.example.ChessGame.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Small controller to gracefully handle common browser requests for resources that may be absent
 * in static files (favicon, chrome devtools config), preventing NoResourceFoundException logs.
 */
@Controller
public class StaticFallbackController {

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        // return 204 No Content to indicate no favicon without triggering an error
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/.well-known/appspecific/com.chrome.devtools.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> devtoolsConfig() {
        // Return an empty JSON object so Chrome DevTools or similar won't log a resource error.
        return ResponseEntity.ok("{}");
    }
}
