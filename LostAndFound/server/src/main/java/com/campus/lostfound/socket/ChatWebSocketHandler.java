package com.campus.lostfound.socket;

import com.campus.lostfound.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // Map<UserId, WebSocketSession>
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // Map<SessionId, Username> for cleanup when principal is null
    private static final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil;

    public ChatWebSocketHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = null;
        Principal principal = session.getPrincipal();
        
        if (principal != null) {
            username = principal.getName();
        } else {
            // Fallback: extract token from query param
            try {
                String query = session.getUri().getQuery();
                if (query != null && query.contains("token=")) {
                    String token = query.split("token=")[1].split("&")[0];
                    username = jwtUtil.extractUsername(token);
                    if (!jwtUtil.validateToken(token, username)) {
                        username = null;
                    }
                }
            } catch (Exception e) {
                System.out.println("WebSocket auth fallback failed: " + e.getMessage());
            }
        }

        if (username != null) {
            sessions.put(username, session);
            sessionUserMap.put(session.getId(), username);
            System.out.println("WebSocket connected: " + username);
        } else {
            System.out.println("WebSocket connection rejected: Unauthorized");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = null;
        Principal principal = session.getPrincipal();
        
        if (principal != null) {
            username = principal.getName();
        } else {
            username = sessionUserMap.get(session.getId());
        }

        if (username != null) {
            sessions.remove(username);
            sessionUserMap.remove(session.getId());
            System.out.println("WebSocket disconnected: " + username);
        }
    }

    public void sendMessageToUser(String username, Object payload) {
        WebSocketSession session = sessions.get(username);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
