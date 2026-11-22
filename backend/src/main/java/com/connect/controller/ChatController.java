package com.connect.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connect.dto.ChatConversationDto;
import com.connect.dto.ChatMessageDto;
import com.connect.dto.ChatPreferenceRequest;
import com.connect.dto.PageResponse;
import com.connect.dto.UpdateMessageRequest;
import com.connect.dto.WebSocketTicketDto;
import com.connect.exception.BadRequestException;
import com.connect.security.AuthenticatedStudent;
import com.connect.service.ChatService;
import com.connect.service.WebSocketTicketService;
import com.connect.websocket.ChatWebSocketHandler;

import jakarta.validation.Valid;

/** REST support for chat: history, read state, message editing and conversation preferences. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final int MAX_PAGE_SIZE = 100;
    private final ChatService chatService;
    private final WebSocketTicketService ticketService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public ChatController(ChatService chatService, WebSocketTicketService ticketService,
                          ChatWebSocketHandler chatWebSocketHandler) {
        this.chatService = chatService;
        this.ticketService = ticketService;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /** Total unread messages, used for the small dot on the Chat navigation item. */
    @GetMapping("/unread")
    public long unread(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return chatService.unreadCount(principal.id());
    }

    @GetMapping("/conversations")
    public List<ChatConversationDto> conversations(
            @AuthenticationPrincipal AuthenticatedStudent principal,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return chatService.conversations(principal.id(), includeArchived);
    }

    @GetMapping("/{otherId}/messages")
    public PageResponse<ChatMessageDto> history(@AuthenticationPrincipal AuthenticatedStudent principal,
            @PathVariable Long otherId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Page must be non-negative and size must be 1 to 100.");
        }
        return chatService.history(principal.id(), otherId, page, size);
    }

    @PostMapping("/{otherId}/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthenticatedStudent principal,
                                         @PathVariable Long otherId) {
        chatService.markRead(principal.id(), otherId);
        return ResponseEntity.noContent().build();
    }

    /** Edits one of your own messages and pushes the update to both participants. */
    @PatchMapping("/messages/{id}")
    public ChatMessageDto editMessage(@AuthenticationPrincipal AuthenticatedStudent principal,
                                      @PathVariable Long id,
                                      @Valid @RequestBody UpdateMessageRequest request) {
        ChatMessageDto updated = chatService.editMessage(principal.id(), id, request.content());
        chatWebSocketHandler.broadcastMessage(updated);
        return updated;
    }

    /** Unsends one of your own messages for both participants. */
    @DeleteMapping("/messages/{id}")
    public ChatMessageDto unsendMessage(@AuthenticationPrincipal AuthenticatedStudent principal,
                                        @PathVariable Long id) {
        ChatMessageDto updated = chatService.unsendMessage(principal.id(), id);
        chatWebSocketHandler.broadcastMessage(updated);
        return updated;
    }

    /** Mute or archive a conversation (caller only). */
    @PatchMapping("/{otherId}/preferences")
    public ResponseEntity<Void> updatePreference(@AuthenticationPrincipal AuthenticatedStudent principal,
                                                 @PathVariable Long otherId,
                                                 @RequestBody ChatPreferenceRequest request) {
        chatService.updatePreference(principal.id(), otherId, request);
        return ResponseEntity.noContent().build();
    }

    /** Clear your view of a conversation and hide it from your inbox. */
    @DeleteMapping("/{otherId}/conversation")
    public ResponseEntity<Void> clearConversation(@AuthenticationPrincipal AuthenticatedStudent principal,
                                                  @PathVariable Long otherId) {
        chatService.clearConversation(principal.id(), otherId);
        return ResponseEntity.noContent().build();
    }

    /** Mint a brief one-use credential for the browser WebSocket handshake. */
    @PostMapping("/socket-ticket")
    public WebSocketTicketDto socketTicket(@AuthenticationPrincipal AuthenticatedStudent principal) {
        return new WebSocketTicketDto(ticketService.issue(principal.id()), ticketService.lifetimeSeconds());
    }
}
