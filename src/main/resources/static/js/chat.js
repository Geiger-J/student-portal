/*
 * Chat system JavaScript for polling-based messaging between tutoring partners.
 * Provides real-time-like experience through efficient polling mechanism.
 * Supports extensible architecture for future WebSocket integration.
 * Handles modal UI interactions and message display formatting.
 */

/**
 * Chat system for tutoring partner communication.
 * Implements polling-based updates with extensible architecture.
 */
class ChatSystem {
    constructor() {
        this.currentRequestId = null;
        this.currentPartnerName = null;
        this.pollingInterval = null;
        this.lastMessageTime = null;
        this.pollingRate = 5000; // 5 seconds - configurable
        this.isPolling = false;
        
        this.initializeElements();
    }

    initializeElements() {
        this.modal = document.getElementById('chatModal');
        this.chatTitle = document.getElementById('chatTitle');
        this.chatMessages = document.getElementById('chatMessages');
        this.chatInput = document.getElementById('chatInput');
        
        // Add enter key listener for chat input
        if (this.chatInput) {
            this.chatInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.sendMessage();
                }
            });
        }
    }

    /**
     * Open chat modal for a specific request.
     */
    async openChat(requestId, partnerName) {
        this.currentRequestId = requestId;
        this.currentPartnerName = partnerName;
        
        // Check if user has access to this chat
        try {
            const accessResponse = await fetch(`/api/chat/request/${requestId}/access`);
            const accessData = await accessResponse.json();
            
            if (!accessData.canAccess) {
                alert('You do not have access to this chat.');
                return;
            }
        } catch (error) {
            console.error('Error checking chat access:', error);
            alert('Unable to access chat. Please try again.');
            return;
        }
        
        // Update modal title
        this.chatTitle.textContent = `Chat with ${partnerName}`;
        
        // Load initial messages
        await this.loadMessages();
        
        // Show modal
        this.modal.style.display = 'block';
        
        // Start polling for updates
        this.startPolling();
        
        // Focus input
        if (this.chatInput) {
            this.chatInput.focus();
        }
    }

    /**
     * Close chat modal and stop polling.
     */
    closeChat() {
        this.modal.style.display = 'none';
        this.stopPolling();
        this.currentRequestId = null;
        this.currentPartnerName = null;
        this.lastMessageTime = null;
    }

    /**
     * Load all messages for the current request.
     */
    async loadMessages() {
        if (!this.currentRequestId) return;

        try {
            const response = await fetch(`/api/chat/request/${this.currentRequestId}/messages`);
            
            if (response.status === 403) {
                alert('Access denied to this chat.');
                this.closeChat();
                return;
            }
            
            if (!response.ok) {
                throw new Error('Failed to load messages');
            }
            
            const messages = await response.json();
            this.displayMessages(messages);
            
            // Update last message time for polling
            if (messages.length > 0) {
                this.lastMessageTime = messages[messages.length - 1].createdAtIso;
            }
            
        } catch (error) {
            console.error('Error loading messages:', error);
            this.chatMessages.innerHTML = '<div class="error-message">Unable to load messages. Please try again.</div>';
        }
    }

    /**
     * Load new messages since last poll.
     */
    async loadNewMessages() {
        if (!this.currentRequestId || !this.lastMessageTime) return;

        try {
            const response = await fetch(`/api/chat/request/${this.currentRequestId}/messages/since?after=${this.lastMessageTime}`);
            
            if (!response.ok) return; // Silently fail for polling
            
            const newMessages = await response.json();
            
            if (newMessages.length > 0) {
                this.appendMessages(newMessages);
                this.lastMessageTime = newMessages[newMessages.length - 1].createdAtIso;
            }
            
        } catch (error) {
            console.error('Error loading new messages:', error);
        }
    }

    /**
     * Send a new message.
     */
    async sendMessage() {
        if (!this.currentRequestId || !this.chatInput) return;

        const content = this.chatInput.value.trim();
        if (!content) return;

        // Disable input while sending
        this.chatInput.disabled = true;

        try {
            const response = await fetch(`/api/chat/request/${this.currentRequestId}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({ content: content })
            });

            if (response.status === 403) {
                alert('You are not authorized to send messages in this chat.');
                this.closeChat();
                return;
            }

            if (!response.ok) {
                throw new Error('Failed to send message');
            }

            const sentMessage = await response.json();
            
            // Clear input
            this.chatInput.value = '';
            
            // Add message to display
            this.appendMessages([sentMessage]);
            this.lastMessageTime = sentMessage.createdAtIso;

        } catch (error) {
            console.error('Error sending message:', error);
            alert('Failed to send message. Please try again.');
        } finally {
            this.chatInput.disabled = false;
            this.chatInput.focus();
        }
    }

    /**
     * Display messages in the chat area.
     */
    displayMessages(messages) {
        if (!this.chatMessages) return;

        this.chatMessages.innerHTML = '';
        this.appendMessages(messages);
    }

    /**
     * Append new messages to the chat area.
     */
    appendMessages(messages) {
        if (!this.chatMessages || !messages.length) return;

        const fragment = document.createDocumentFragment();

        messages.forEach(message => {
            const messageElement = this.createMessageElement(message);
            fragment.appendChild(messageElement);
        });

        this.chatMessages.appendChild(fragment);
        this.scrollToBottom();
    }

    /**
     * Create a message element.
     */
    createMessageElement(message) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `chat-message ${message.currentUser ? 'own-message' : 'partner-message'}`;

        const timeString = new Date(message.createdAtIso).toLocaleTimeString();
        
        messageDiv.innerHTML = `
            <div class="message-header">
                <span class="sender-name">${message.senderName}</span>
                <span class="message-time">${timeString}</span>
            </div>
            <div class="message-content">${this.escapeHtml(message.content)}</div>
        `;

        return messageDiv;
    }

    /**
     * Escape HTML to prevent XSS.
     */
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Scroll chat messages to bottom.
     */
    scrollToBottom() {
        if (this.chatMessages) {
            this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
        }
    }

    /**
     * Start polling for new messages.
     */
    startPolling() {
        if (this.isPolling) return;
        
        this.isPolling = true;
        this.pollingInterval = setInterval(() => {
            this.loadNewMessages();
        }, this.pollingRate);
    }

    /**
     * Stop polling for new messages.
     */
    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
        this.isPolling = false;
    }
}

// Initialize chat system when DOM is loaded
let chatSystem;

document.addEventListener('DOMContentLoaded', function() {
    chatSystem = new ChatSystem();
});

// Global functions for template usage
function openChat(requestId, partnerName) {
    if (chatSystem) {
        chatSystem.openChat(requestId, partnerName);
    }
}

function closeChat() {
    if (chatSystem) {
        chatSystem.closeChat();
    }
}

function sendMessage() {
    if (chatSystem) {
        chatSystem.sendMessage();
    }
}

// TODO: WebSocket integration for future upgrade
// When implementing WebSocket, replace polling with:
//
// class WebSocketChatSystem extends ChatSystem {
//     constructor() {
//         super();
//         this.socket = null;
//     }
//
//     connectWebSocket(requestId) {
//         this.socket = new WebSocket(`/ws/chat/${requestId}`);
//         this.socket.onmessage = (event) => {
//             const message = JSON.parse(event.data);
//             this.appendMessages([message]);
//         };
//     }
//
//     sendMessage() {
//         // Send via WebSocket instead of HTTP
//         if (this.socket && this.socket.readyState === WebSocket.OPEN) {
//             this.socket.send(JSON.stringify({
//                 content: this.chatInput.value.trim(),
//                 requestId: this.currentRequestId
//             }));
//         }
//     }
// }