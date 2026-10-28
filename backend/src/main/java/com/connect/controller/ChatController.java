package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.ChatConversationDto;
import com.connect.dto.ChatMessageDto;
import com.connect.dto.PageResponse;
import com.connect.dto.WebSocketTicketDto;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.ChatService;
import com.connect.service.WebSocketTicketService;

/** REST support for direct chat: conversations, paginated history, and read state. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final int MAX_PAGE_SIZE = 100;
    private final ChatService chatService;
    private final WebSocketTicketService ticketService;

    public ChatController(ChatService chatService, WebSocketTicketService ticketService) {
        this.chatService = chatService;
        this.ticketService = ticketService;
    }

    @GetMapping("/conversations")
    public List<ChatConversationDto> conversations(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return chatService.conversations(principal.id());
    }

    @GetMapping("/{otherId}/messages")
    public PageResponse<ChatMessageDto> history(@AuthenticationPrincipal AuthenticatedStudent principal,
            @PathVariable Long otherId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new com.connect.exception.BadRequestException("Page must be non-negative and size must be 1 to 100.");
        }
        return chatService.history(principal.id(), otherId, page, size);
    }

    @PostMapping("/{otherId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthenticatedStudent principal,
                                          @PathVariable Long otherId) {
        chatService.markRead(principal.id(), otherId);
        return ResponseEntity.noContent().build();
    }

    /** Mint a brief one-use credential for the browser WebSocket handshake. */
    @PostMapping("/socket-ticket")
    public WebSocketTicketDto socketTicket(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return new WebSocketTicketDto(ticketService.issue(principal.id()), ticketService.lifetimeSeconds());
    }
}
