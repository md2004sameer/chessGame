package com.example.ChessGame.controller;

import com.example.ChessGame.controller.dto.MoveResponse;
import com.example.ChessGame.entity.Game;
import com.example.ChessGame.entity.Room;
import com.example.ChessGame.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class SubscriptionEventListener implements ApplicationListener<SessionSubscribeEvent> {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionEventListener.class);
    private final RoomService roomService;
    private final com.example.ChessGame.service.MessagingService messagingService;

    public SubscriptionEventListener(RoomService roomService, com.example.ChessGame.service.MessagingService messagingService) {
        this.roomService = roomService;
        this.messagingService = messagingService;
    }

    @Override
    public void onApplicationEvent(@NonNull SessionSubscribeEvent event) {
        String dest = SimpMessageHeaderAccessor.getDestination(event.getMessage().getHeaders());
        if (dest == null) return;
        if (!dest.startsWith("/topic/rooms/")) return;
        String roomId = dest.substring("/topic/rooms/".length());
        logger.info("Subscription to {} detected, broadcasting current state", dest);
        // Build and broadcast state (safe to broadcast to topic; subscribers including the new one will receive it)
        Room room = roomService.getRoom(roomId);
        Game game = roomService.getGame(roomId);
        MoveResponse resp = RoomStateBuilder.build(room, game);
        messagingService.sendToTopic(dest, resp);
    }
}
