package com.gkglobal.chatbot.dto;

/** Incoming chat request body: {@code {"message": "..."}}. */
public record ChatRequest(String message) {
}