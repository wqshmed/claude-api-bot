package com.gkglobal.chatbot.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the user's personal-assistant context from a JSON file once at startup
 * and exposes it as raw text to be embedded in the system prompt.
 *
 * <p>The file location is configurable via {@code assistant.context-file}
 * (default: {@code classpath:assistant-context.json}). Point it at a
 * {@code file:/absolute/path.json} to edit details without rebuilding — note
 * the file is read once at boot, so a restart is needed to pick up changes.
 *
 * <p>The content is stable across requests, so it sits in the cached prefix of
 * the system prompt (see {@link ChatService}).
 */
@Component
public class AssistantContext {

    private final ResourceLoader resourceLoader;
    private final String location;
    private String details = "";

    public AssistantContext(ResourceLoader resourceLoader,
                            @Value("${assistant.context-file:classpath:assistant-context.json}") String location) {
        this.resourceLoader = resourceLoader;
        this.location = location;
    }

    @PostConstruct
    void load() {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            details = new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read assistant context from " + location, e);
        }
    }

    /** Whether any context was loaded. */
    public boolean isPresent() {
        return !details.isBlank();
    }

    /** Raw JSON text to embed in the system prompt. */
    public String details() {
        return details;
    }
}