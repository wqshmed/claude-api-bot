# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A minimal Spring Boot REST service that wraps the **Claude API** via the official
**Anthropic Java SDK** (`com.anthropic:anthropic-java`). It exposes one endpoint,
`POST /api/chat`, that forwards a user message to Claude and returns the text reply.
Built as a learning chatbot.

## Commands

```bash
# Requires ANTHROPIC_API_KEY in the environment (the SDK reads it via fromEnv()).
export ANTHROPIC_API_KEY="sk-ant-..."

mvn -DskipTests compile      # compile
mvn spring-boot:run          # run on http://localhost:8080
mvn test                     # run all tests
mvn -Dtest=ClassName#method test   # run a single test
mvn clean package            # build the executable jar (target/claude-api-bot-*.jar)

# Smoke-test the endpoint once running:
curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" \
  -d '{"message": "hello"}'
```

## ⚠️ Known blocker: Java 21 vs Spring Boot version

The local toolchain is **JDK 21** (`java.version=26` in `pom.xml`), but
**Spring Boot 3.4.5 cannot boot on Java 21** — its bundled ASM fails with
`Unsupported class file major version 70` during classpath scanning at
`spring-boot:run`. The project *compiles* fine but does not *start*.

To run, the Spring Boot version must be one whose ASM understands Java 21
bytecode (a Spring Boot release published after Java 21). When changing this,
bump `<version>` on `spring-boot-starter-parent` in `pom.xml` and re-verify boot
with `mvn spring-boot:run`, not just `mvn compile`. Alternatively, lower
`java.version` to a release the current Spring Boot supports.

## Architecture

Request flow: `ChatController` (`POST /api/chat`) → `ChatService` → Anthropic SDK
→ Claude API. DTOs (`ChatRequest`/`ChatResponse`) are Java records.

- **`config/AnthropicConfig`** — single `AnthropicClient` bean built with
  `AnthropicOkHttpClient.fromEnv()`. The client is reused across all requests.
- **`service/ChatService`** — builds `MessageCreateParams` and calls
  `client.messages().create(...)`. Key choices:
  - Model and max-tokens come from `application.properties`
    (`anthropic.model`, `anthropic.max-tokens`), injected via `@Value`.
  - **Adaptive thinking** is enabled (`ThinkingConfigAdaptive`); only text blocks
    are collected from the response (`block.text()` stream), thinking is ignored.
  - The system prompt carries a `cache_control` breakpoint
    (`systemOfTextBlockParams` + `CacheControlEphemeral`). Caching only activates
    once the cached prefix exceeds the model minimum (~4096 tokens for Opus), so
    the current short prompt does not cache yet — the breakpoint is placed
    correctly for when the prompt/context grows.

## Conventions

- **groupId vs package:** Maven `groupId` is `com.gk-global` (hyphen is valid in
  coordinates); Java source uses package `com.gkglobal.chatbot` because package
  names cannot contain hyphens. Keep new source under `com.gkglobal.chatbot`.
- Default model is `claude-opus-4-8`; switch to `claude-haiku-4-5` via
  `application.properties` for cheaper/faster runs.
