package com.gkglobal.chatbot.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory history of chat turns, keyed by conversation id.
 *
 * <p>This keeps multi-turn context for the lifetime of the process only — there
 * is no persistence, eviction, or per-conversation size cap yet. Fine for a
 * learning chatbot; swap for a real store (Redis, a DB) before relying on it in
 * production, where unbounded growth and lost-on-restart history would matter.
 */
@Component
public class ConversationStore {

    /** Who spoke a turn — maps to the user/assistant roles the Claude API expects. */
    public enum Role { USER, ASSISTANT }

    /** A single turn of the conversation. */
    public record Turn(Role role, String text) {
    }

    private final Map<String, List<Turn>> conversations = new ConcurrentHashMap<>();

    /** Ordered history for a conversation, oldest first; empty if unknown. */
    public List<Turn> history(String conversationId) {
        return conversations.getOrDefault(conversationId, List.of());
    }

    /** Appends a turn, creating the conversation on first use. */
    public void append(String conversationId, Turn turn) {
        conversations
                .computeIfAbsent(conversationId, key -> new CopyOnWriteArrayList<>())
                .add(turn);
    }
}