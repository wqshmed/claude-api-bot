package com.gkglobal.chatbot.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    /**
     * Stable system prompt — defines the assistant's persona.
     *
     * <p>It carries a {@code cache_control} breakpoint so the prefix
     * (tools + system) is cached and reused across requests. Note: prompt
     * caching only activates once the cached prefix exceeds the model's
     * minimum (~4096 tokens for Opus). This short prompt won't cache on its
     * own yet — the breakpoint is in the right place so caching kicks in
     * automatically once you grow the system prompt or prepend large,
     * stable context (docs, examples, instructions).
     */
    private static final String SYSTEM_PROMPT = """
            You are a friendly, patient programming tutor that helps people learn.
            Explain concepts clearly and concisely, use small concrete examples,
            and prefer plain language over jargon. When you show code, keep it
            minimal and runnable. If a question is ambiguous, ask one brief
            clarifying question before answering.
            """;

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public ChatService(AnthropicClient client,
                        @Value("${anthropic.model}") String model,
                        @Value("${anthropic.max-tokens}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends a single user message to Claude and returns the text reply.
     *
     * <p>Adaptive thinking is enabled: Claude decides how much to reason per
     * request. Thinking blocks are not surfaced here — we collect only the
     * text blocks of the response.
     */
    public String reply(String userMessage) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(SYSTEM_PROMPT)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()))
                .addUserMessage(userMessage)
                .build();

        Message response = client.messages().create(params);

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining());
    }
}