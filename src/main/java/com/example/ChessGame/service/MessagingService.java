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
}
