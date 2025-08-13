package com.example.student_portal.service;

/*
 * Service layer for chat messaging functionality between tutoring partners.
 * Implements security checks ensuring only participants can access conversations.
 * Provides polling-friendly API with efficient message retrieval.
 * Maintains extensible architecture for future WebSocket integration.
 */

import com.example.student_portal.dto.ChatMessageDto;
import com.example.student_portal.entity.ChatMessage;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import com.example.student_portal.entity.Match;
import com.example.student_portal.repository.ChatMessageRepository;
import com.example.student_portal.repository.RequestRepository;
import com.example.student_portal.repository.MatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages between tutoring partners.
 * Ensures security by validating user participation in conversations.
 */
@Service
@Transactional
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RequestRepository requestRepository;
    private final MatchRepository matchRepository;
    private final UserService userService;

    public ChatService(ChatMessageRepository chatMessageRepository, 
                      RequestRepository requestRepository,
                      MatchRepository matchRepository,
                      UserService userService) {
        this.chatMessageRepository = chatMessageRepository;
        this.requestRepository = requestRepository;
        this.matchRepository = matchRepository;
        this.userService = userService;
    }

    /**
     * Get all messages for a request if user has permission.
     * Only participants in the tutoring match can access messages.
     */
    public List<ChatMessageDto> getMessages(Long requestId, String userEmail) {
        if (!canAccessChat(requestId, userEmail)) {
            throw new SecurityException("User not authorized to access this chat");
        }

        List<ChatMessage> messages = chatMessageRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        return messages.stream()
                .map(message -> ChatMessageDto.from(message, userEmail))
                .collect(Collectors.toList());
    }

    /**
     * Get messages after a specific timestamp for polling updates.
     */
    public List<ChatMessageDto> getMessagesAfter(Long requestId, Instant after, String userEmail) {
        if (!canAccessChat(requestId, userEmail)) {
            throw new SecurityException("User not authorized to access this chat");
        }

        List<ChatMessage> messages = chatMessageRepository.findByRequestIdAndCreatedAtAfterOrderByCreatedAtAsc(requestId, after);
        return messages.stream()
                .map(message -> ChatMessageDto.from(message, userEmail))
                .collect(Collectors.toList());
    }

    /**
     * Send a new message in the chat.
     */
    public ChatMessageDto sendMessage(Long requestId, String content, String userEmail) {
        if (!canAccessChat(requestId, userEmail)) {
            throw new SecurityException("User not authorized to send messages in this chat");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (content.length() > 1000) {
            throw new IllegalArgumentException("Message too long (max 1000 characters)");
        }

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        User user = userService.findByEmail(userEmail);

        ChatMessage message = new ChatMessage(request, user, content.trim());
        ChatMessage savedMessage = chatMessageRepository.save(message);

        return ChatMessageDto.from(savedMessage, userEmail);
    }

    /**
     * Check if a user can access chat for a specific request.
     * User must be either the tutor or tutee in a matched request.
     */
    public boolean canAccessChat(Long requestId, String userEmail) {
        User user = userService.findByEmail(userEmail);
        
        // Find if there's an active match for this request
        List<Match> matches = matchRepository.findByTutorRequestIdOrTuteeRequestId(requestId, requestId);
        
        for (Match match : matches) {
            Long tutorUserId = match.getTutorRequest().getUser().getId();
            Long tuteeUserId = match.getTuteeRequest().getUser().getId();
            
            if (user.getId().equals(tutorUserId) || user.getId().equals(tuteeUserId)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get message count for a request.
     */
    public long getMessageCount(Long requestId, String userEmail) {
        if (!canAccessChat(requestId, userEmail)) {
            return 0;
        }
        return chatMessageRepository.countByRequestId(requestId);
    }

    /**
     * Delete all messages for a request (admin function).
     */
    public void deleteAllMessages(Long requestId) {
        chatMessageRepository.deleteByRequestId(requestId);
    }

    // TODO: Future WebSocket implementation
    // When implementing WebSocket, add these methods:
    // - broadcastMessage(Long requestId, ChatMessageDto message)
    // - subscribeToChat(Long requestId, String userEmail)
    // - unsubscribeFromChat(Long requestId, String userEmail)
    // 
    // WebSocket configuration would replace polling mechanism with:
    // - @MessageMapping for incoming messages
    // - @SendTo for broadcasting to subscribers
    // - SimpMessagingTemplate for programmatic messaging
}