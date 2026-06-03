package com.gkglobal.chatbot.dto;

/** Outgoing chat response body: {@code {"reply": "..."}}. */
public record ChatResponse(String reply) {
}