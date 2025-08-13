package com.example.student_portal.entity;

/*
 * Chat message entity for in-app messaging between matched tutoring pairs.
 * Supports polling-based communication with extensible architecture.
 * Associates messages with specific tutoring requests for context.
 * Provides foundation for real-time messaging system upgrade.
 */

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Represents a chat message between users in a tutoring context.
 * Messages are scoped to specific requests to maintain conversation context.
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The tutoring request this message relates to.
     * This provides context and access control for the conversation.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Request request;

    /**
     * The user who sent this message.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * The message content.
     */
    @NotNull
    @Size(min = 1, max = 1000)
    @Column(nullable = false, length = 1000)
    private String content;

    /**
     * When the message was created.
     */
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructors
    public ChatMessage() {
        this.createdAt = Instant.now();
    }

    public ChatMessage(Request request, User sender, String content) {
        this();
        this.request = request;
        this.sender = sender;
        this.content = content;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", request=" + (request != null ? request.getId() : null) +
                ", sender=" + (sender != null ? sender.getEmail() : null) +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}