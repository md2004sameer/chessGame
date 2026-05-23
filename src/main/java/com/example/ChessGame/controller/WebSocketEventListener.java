package com.example.ChessGame.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();
        logger.info("STOMP session connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String reason = event.getCloseStatus() != null ? event.getCloseStatus().toString() : "";
        logger.info("STOMP session disconnected: sessionId={}, reason={}", sessionId, reason);
    }
}
