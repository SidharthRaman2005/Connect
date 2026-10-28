package com.connect.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.connect.service.WebSocketTicketService;
import com.connect.websocket.ChatWebSocketHandler;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final WebSocketTicketService tickets;
    private final List<String> allowedOrigins;

    public ChatWebSocketConfig(ChatWebSocketHandler handler, WebSocketTicketService tickets,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        this.handler = handler;
        this.tickets = tickets;
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/chat")
                .addInterceptors(new TicketHandshakeInterceptor(tickets))
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    private static final class TicketHandshakeInterceptor implements HandshakeInterceptor {
        private final WebSocketTicketService tickets;
        private TicketHandshakeInterceptor(WebSocketTicketService tickets) { this.tickets = tickets; }

        @Override
        public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler, @NonNull java.util.Map<String, Object> attributes) {
            String query = request.getURI().getRawQuery();
            if (query == null) return false;
            String ticket = Arrays.stream(query.split("&"))
                    .filter(part -> part.startsWith("ticket="))
                    .map(part -> part.substring("ticket=".length())).findFirst().orElse(null);
            Long studentId = tickets.consume(ticket);
            if (studentId == null) return false;
            attributes.put("studentId", studentId);
            return true;
        }

        @Override
        public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                @NonNull WebSocketHandler wsHandler, Exception exception) {}
    }
}
