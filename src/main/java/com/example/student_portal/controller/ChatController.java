package com.example.student_portal.controller;

/*
 * REST API controller for chat messaging between tutoring partners.
 * Provides JSON endpoints for polling-based chat implementation.
 * Ensures security through authentication and authorization checks.
 * Supports extensible architecture for future WebSocket upgrade.
 */

import com.example.student_portal.dto.ChatMessageDto;
import com.example.student_portal.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for chat functionality.
 * Provides endpoints for retrieving and sending chat messages.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Get all messages for a specific request.
     * GET /api/chat/request/{id}/messages
     */
    @GetMapping("/request/{requestId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails principal) {
        
        try {
            List<ChatMessageDto> messages = chatService.getMessages(requestId, principal.getUsername());
            return ResponseEntity.ok(messages);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get messages after a specific timestamp for polling updates.
     * GET /api/chat/request/{id}/messages?after=2025-01-15T10:30:00Z
     */
    @GetMapping("/request/{requestId}/messages/since")
    public ResponseEntity<List<ChatMessageDto>> getMessagesAfter(
            @PathVariable Long requestId,
            @RequestParam String after,
            @AuthenticationPrincipal UserDetails principal) {
        
        try {
            Instant afterInstant = Instant.parse(after);
            List<ChatMessageDto> messages = chatService.getMessagesAfter(requestId, afterInstant, principal.getUsername());
            return ResponseEntity.ok(messages);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.status(400).build();
        }
    }

    /**
     * Send a new message.
     * POST /api/chat/request/{id}/messages
     * Body: { "content": "Hello, when would you like to meet?" }
     */
    @PostMapping("/request/{requestId}/messages")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal UserDetails principal) {
        
        try {
            String content = requestBody.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ChatMessageDto message = chatService.sendMessage(requestId, content, principal.getUsername());
            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Check if user can access chat for a request.
     * GET /api/chat/request/{id}/access
     */
    @GetMapping("/request/{requestId}/access")
    public ResponseEntity<Map<String, Boolean>> checkAccess(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails principal) {
        
        boolean canAccess = chatService.canAccessChat(requestId, principal.getUsername());
        return ResponseEntity.ok(Map.of("canAccess", canAccess));
    }

    /**
     * Get message count for a request.
     * GET /api/chat/request/{id}/count
     */
    @GetMapping("/request/{requestId}/count")
    public ResponseEntity<Map<String, Long>> getMessageCount(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails principal) {
        
        try {
            long count = chatService.getMessageCount(requestId, principal.getUsername());
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // TODO: WebSocket endpoints for future implementation
    // When upgrading to WebSocket, replace polling with:
    // @MessageMapping("/chat/{requestId}/send")
    // @SendTo("/topic/chat/{requestId}")
    // public ChatMessageDto handleMessage(@DestinationVariable Long requestId, ChatMessageDto message, Principal principal)
    //
    // @SubscribeMapping("/chat/{requestId}")
    // public List<ChatMessageDto> subscribe(@DestinationVariable Long requestId, Principal principal)
}