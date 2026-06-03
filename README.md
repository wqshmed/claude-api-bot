# claude-api-bot

A minimal Spring Boot learning chatbot that calls the **Claude API** through the
official **Anthropic Java SDK**. Exposes one HTTP endpoint you can hit with `curl`
or any client.

- **Group ID:** `com.gk-global`
- **Java:** 26
- **Spring Boot:** 3.4.5
- **Anthropic Java SDK:** 2.34.0
- **Default model:** `claude-opus-4-8` (configurable)

> Java packages can't contain hyphens, so the Maven `groupId` is `com.gk-global`
> while the source package is `com.gkglobal.chatbot`.

## Project layout

```
src/main/java/com/gkglobal/chatbot/
├── ChatbotApplication.java        # Spring Boot entry point
├── config/AnthropicConfig.java    # Builds the AnthropicClient bean
├── controller/ChatController.java # POST /api/chat
├── service/ChatService.java       # Calls the Claude API
└── dto/                           # ChatRequest / ChatResponse records
src/main/resources/application.properties
```

## Prerequisites

- JDK 26
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

```bash
curl -s http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain what a Java record is, with a tiny example."}'
```

Response:

```json
{ "reply": "A record is a compact class for immutable data ..." }
```

## Configuration

Edit `src/main/resources/application.properties`:

| Property              | Default            | Notes                                            |
| --------------------- | ------------------ | ------------------------------------------------ |
| `anthropic.model`     | `claude-opus-4-8`  | Set to `claude-haiku-4-5` for cheaper/faster use |
| `anthropic.max-tokens`| `8192`             | Max generated tokens (includes thinking tokens)  |
| `server.port`         | `8080`             | HTTP port                                        |

## How it works

- **`AnthropicConfig`** creates one reusable `AnthropicClient` (`fromEnv()` reads
  `ANTHROPIC_API_KEY`).
- **`ChatService`** builds a `MessageCreateParams` request with a cached system
  prompt and **adaptive thinking** enabled (Claude decides how much to reason per
  request), then collects the text blocks from the response.
- **`ChatController`** maps `POST /api/chat` to the service.

### A note on prompt caching

The system prompt is sent with a `cache_control` breakpoint — the correct place
for it. However, prompt caching only activates once the cached prefix exceeds the
model's minimum (~4096 tokens for Opus). The short tutor prompt here won't cache
on its own yet; caching begins paying off automatically once you grow the system
prompt or prepend large, stable context (reference docs, few-shot examples).
Verify hits via `response.usage().cacheReadInputTokens()`.

## Ideas to extend

- **Multi-turn chat:** keep a conversation history and send prior turns each request.
- **Streaming:** use `client.messages().createStreaming(...)` to stream tokens to the client (e.g. SSE).
- **Structured output:** constrain replies to a schema with `outputConfig(...)`.