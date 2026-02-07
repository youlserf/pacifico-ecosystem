package com.pacifico.issuance.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time policy issuance notifications.
 * <p>
 * This component manages active WebSocket sessions mapped by customer DNI.
 * It allows the {@link IssuanceService} to push updates directly to the 
 * front-end when a policy is formally issued.
 */
@Component
public class IssuanceWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(IssuanceWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String dni = getDniFromSession(session);
        if (dni != null) {
            sessions.put(dni, session);
            logger.info("WebSocket session established for DNI: {}", dni);
        } else {
            logger.warn("WebSocket session established without DNI. Closing...");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String dni = getDniFromSession(session);
        if (dni != null) {
            sessions.remove(dni);
            logger.info("WebSocket session closed for DNI: {}", dni);
        }
    }

    public void sendToUser(String dni, String payload) {
        WebSocketSession session = sessions.get(dni);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
                logger.info("Pushed notification to DNI: {}", dni);
            } catch (IOException e) {
                logger.error("Error pushing to WebSocket", e);
            }
        } else {
            logger.warn("No active session for DNI: {}", dni);
        }
    }

    private String getDniFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("dni=")) {
            return query.split("dni=")[1].split("&")[0];
        }
        return null;
    }
}
