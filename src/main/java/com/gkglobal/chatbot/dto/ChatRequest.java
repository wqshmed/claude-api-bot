package com.gkglobal.chatbot.dto;

/**
 * Incoming chat request body: {@code {"message": "...", "conversationId": "..."}}.
 *
 * <p>{@code conversationId} is optional. Omit it (or send blank) to start a new
 * conversation; the reply carries the server-generated id to continue with.
 */
public record ChatRequest(String message, String conversationId) {
}