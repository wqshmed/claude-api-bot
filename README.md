# claude-api-bot

A minimal Spring Boot learning chatbot that calls the **Claude API** through the
official **Anthropic Java SDK**. Exposes one HTTP endpoint you can hit with `curl`
or any client, with **multi-turn conversation** support (Claude remembers the
prior turns of a conversation).

- **Group ID:** `com.gk-global`
- **Java:** 21+
- **Spring Boot:** 3.4.5
- **Anthropic Java SDK:** 2.34.0
- **Default model:** `claude-opus-4-8` (configurable)

> Java packages can't contain hyphens, so the Maven `groupId` is `com.gk-global`
> while the source package is `com.gkglobal.chatbot`.

## Project layout

```
src/main/java/com/gkglobal/chatbot/
├── ChatbotApplication.java          # Spring Boot entry point
├── config/AnthropicConfig.java      # Builds the AnthropicClient bean
├── controller/ChatController.java   # POST /api/chat
├── service/ChatService.java         # Calls the Claude API, replays history
├── service/ConversationStore.java   # In-memory per-conversation history
└── dto/                             # ChatRequest / ChatResponse records
src/main/resources/
├── application.yml                  # Base config (default profile: local)
└── application-{local,dev,stage,prod,test}.yml   # Per-profile overrides
```

## Prerequisites

- JDK 21+
- Maven 3.9+ (or use your IDE)
- An Anthropic API key

## Setup

Set your API key in the environment (the SDK reads `ANTHROPIC_API_KEY`):

```bash
export ANTHROPIC_API_KEY="sk-ant-..."
```

## Run

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

## Try it

Start a new conversation (omit `conversationId`):

```bash
curl -s http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "My name is Waqas. Remember it."}'
```

Response — note the server-generated `conversationId`:

```json
{ "reply": "Got it, Waqas! ...", "conversationId": "3f9c1a2b-...." }
```

Continue the same conversation by sending that id back:

```bash
curl -s http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is my name?", "conversationId": "3f9c1a2b-...."}'
```

```json
{ "reply": "Your name is Waqas.", "conversationId": "3f9c1a2b-...." }
```

Omit the `conversationId` (or send a new one) and Claude starts fresh with no
memory of prior turns.

## Configuration

Base config lives in `src/main/resources/application.yml`; per-environment
overrides live in `application-{profile}.yml`. The default profile is `local`;
pick another with `--spring.profiles.active=dev` or `SPRING_PROFILES_ACTIVE=dev`.

| Property              | Base default       | Notes                                            |
| --------------------- | ------------------ | ------------------------------------------------ |
| `anthropic.model`     | `claude-opus-4-8`  | `dev` profile uses `claude-haiku-4-5` (cheaper/faster) |
| `anthropic.max-tokens`| `500`              | Max generated tokens (includes thinking tokens); per-profile |
| `anthropic.api-key`   | `${ANTHROPIC_API_KEY}` | Bound from the environment for `@Value`     |
| `server.port`         | `8080`             | HTTP port                                        |

## How it works

- **`AnthropicConfig`** creates one reusable `AnthropicClient` (`fromEnv()` reads
  `ANTHROPIC_API_KEY`).
- **`ChatService`** builds a `MessageCreateParams` request with a cached system
  prompt and **adaptive thinking** enabled (Claude decides how much to reason per
  request), then collects the text blocks from the response.
- **`ConversationStore`** holds each conversation's turns in memory, keyed by
  `conversationId`. On every request `ChatService` replays the stored turns,
  appends the new user message, then saves the user + assistant turns back. The
  Claude API is stateless, so multi-turn memory means resubmitting the transcript
  each turn.
- **`ChatController`** maps `POST /api/chat` to the service and threads the
  `conversationId` through.

> ⚠️ Conversation history is **in-memory only** — it is not persisted, has no
> eviction, and is lost on restart. Each conversation also grows unbounded.
> Swap `ConversationStore` for a real store (Redis, a DB) and add a history cap
> before relying on this in production.

### A note on prompt caching

The system prompt is sent with a `cache_control` breakpoint — the correct place
for it. However, prompt caching only activates once the cached prefix exceeds the
model's minimum (~4096 tokens for Opus). The short tutor prompt here won't cache
on its own yet; caching begins paying off automatically once you grow the system
prompt or prepend large, stable context (reference docs, few-shot examples).
Verify hits via `response.usage().cacheReadInputTokens()`.

## Ideas to extend

- **Persistent / bounded history:** back `ConversationStore` with Redis or a DB, and cap turns per conversation.
- **Streaming:** use `client.messages().createStreaming(...)` to stream tokens to the client (e.g. SSE).
- **Structured output:** constrain replies to a schema with `outputConfig(...)`.