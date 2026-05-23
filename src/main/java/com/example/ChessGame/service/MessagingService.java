package com.example.ChessGame.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessagingService {
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    private final SimpMessagingTemplate template;

    public MessagingService(SimpMessagingTemplate template) {
        this.template = template;
    }

    /**
     * Safely send a payload to a destination topic. Returns true if send was attempted successfully, false otherwise.
     */
    public boolean sendToTopic(String destination, Object payload) {
        if (destination == null || payload == null) {
            logger.warn("Attempted to send null destination or payload: dest={}, payload={}", destination, payload);
            return false;
        }
        try {
            template.convertAndSend(destination, payload);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send message to {}: {}", destination, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send a payload to a specific user session destination (private message).
     * The destination should be the user-specific destination path (e.g. "/queue/rooms/{roomId}/token").
     */
    public boolean sendToUser(String sessionId, String destination, Object payload) {
        if (sessionId == null || destination == null || payload == null) {
            logger.warn("Attempted to sendToUser with null sessionId/destination/payload: {}, {}, {}", sessionId, destination, payload);
            return false;
        }
        try {
            org.springframework.messaging.simp.SimpMessageHeaderAccessor sha = org.springframework.messaging.simp.SimpMessageHeaderAccessor.create(org.springframework.messaging.simp.SimpMessageType.MESSAGE);
            sha.setSessionId(sessionId);
            sha.setLeaveMutable(true);
            // Primary: try convertAndSendToUser with session header
            template.convertAndSendToUser(sessionId, destination, payload, sha.getMessageHeaders());
            // Fallback: also send to the explicit /user/{sessionId}{destination} path
            try {
                template.convertAndSend("/user/" + sessionId + destination, payload);
            } catch (Exception ignore) {
                // best-effort fallback; ignore failures here
            }
            return true;
        } catch (Exception e) {
            logger.error("Failed to send private message to {} on {}: {}", sessionId, destination, e.getMessage(), e);
            return false;
        }
    }
}
