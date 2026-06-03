package com.gkglobal.chatbot.dto;

/**
 * Outgoing chat response body: {@code {"reply": "...", "conversationId": "..."}}.
 *
 * <p>{@code conversationId} echoes (or, for a new conversation, supplies) the id
 * the client should send on the next turn to keep the same thread.
 */
public record ChatResponse(String reply, String conversationId) {
}