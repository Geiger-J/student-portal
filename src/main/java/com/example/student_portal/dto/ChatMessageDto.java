package com.example.student_portal.dto;

/*
 * Data transfer object for chat messages in API responses.
 * Provides clean JSON structure for frontend polling system.
 * Includes user-friendly fields while maintaining security boundaries.
 * Supports extensible chat features and message metadata.
 */

import com.example.student_portal.entity.ChatMessage;

import java.time.Instant;

/**
 * DTO for transferring chat message data to the frontend.
 * Contains only necessary information for display and interaction.
 */
public class ChatMessageDto {
    
    private Long id;
    private String senderName;
    private String senderEmail;
    private String content;
    private String createdAtIso;
    private boolean isCurrentUser;

    // Default constructor
    public ChatMessageDto() {}

    // Constructor for mapping from entity
    public ChatMessageDto(ChatMessage message, String currentUserEmail) {
        this.id = message.getId();
        this.senderName = message.getSender().getFullName();
        this.senderEmail = message.getSender().getEmail();
        this.content = message.getContent();
        this.createdAtIso = message.getCreatedAt().toString();
        this.isCurrentUser = message.getSender().getEmail().equals(currentUserEmail);
    }

    // Static factory method
    public static ChatMessageDto from(ChatMessage message, String currentUserEmail) {
        return new ChatMessageDto(message, currentUserEmail);
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAtIso() {
        return createdAtIso;
    }

    public void setCreatedAtIso(String createdAtIso) {
        this.createdAtIso = createdAtIso;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public void setCurrentUser(boolean currentUser) {
        isCurrentUser = currentUser;
    }

    @Override
    public String toString() {
        return "ChatMessageDto{" +
                "id=" + id +
                ", senderName='" + senderName + '\'' +
                ", content='" + content + '\'' +
                ", createdAtIso='" + createdAtIso + '\'' +
                ", isCurrentUser=" + isCurrentUser +
                '}';
    }
}