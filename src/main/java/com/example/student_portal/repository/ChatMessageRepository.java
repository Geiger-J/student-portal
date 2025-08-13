package com.example.student_portal.repository;

/*
 * Repository for ChatMessage entity supporting chat functionality.
 * Provides efficient query methods for message retrieval and management.
 * Supports ordering by creation time for proper conversation flow.
 * Enables security checks through request-based message filtering.
 */

import com.example.student_portal.entity.ChatMessage;
import com.example.student_portal.entity.Request;
import com.example.student_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for ChatMessage entity.
 * Provides access to chat messages with ordering and filtering capabilities.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all messages for a specific request, ordered by creation time.
     * This provides the conversation history for a tutoring session.
     */
    List<ChatMessage> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    /**
     * Find messages for a request after a specific timestamp.
     * Useful for polling updates without retrieving entire conversation.
     */
    List<ChatMessage> findByRequestIdAndCreatedAtAfterOrderByCreatedAtAsc(Long requestId, Instant after);

    /**
     * Count messages in a specific request.
     * Useful for conversation statistics.
     */
    long countByRequestId(Long requestId);

    /**
     * Find recent messages for a request (useful for preview).
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.request.id = :requestId ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesByRequestId(@Param("requestId") Long requestId);

    /**
     * Check if a user has sent any messages in a request.
     * Useful for access control validation.
     */
    boolean existsByRequestIdAndSenderId(Long requestId, Long senderId);

    /**
     * Delete all messages for a specific request.
     * Useful for cleanup when requests are deleted.
     */
    void deleteByRequestId(Long requestId);
}