package com.gkglobal.chatbot.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnthropicConfig {

    /**
     * Builds a single, reusable Anthropic client.
     *
     * <p>{@code fromEnv()} reads the API key from the {@code ANTHROPIC_API_KEY}
     * environment variable. Never hardcode the key in source.
     */
    /*@Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder().apiKey("put api key here for quick testing").build();
    }*/


    @Value("${anthropic.api-key}")
    private String apiKey;

    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }

}