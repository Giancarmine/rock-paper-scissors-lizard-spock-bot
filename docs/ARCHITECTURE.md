# Architecture

This document describes how the bot is put together, how a Telegram update travels
through the system, and where to extend it.

## Context

```mermaid
flowchart LR
    User[Telegram user] <-->|messages, buttons| TG[Telegram Bot API]
    TG <-->|long polling over HTTPS| Bot[Bot process<br/>Ktor Netty :$PORT]
    Health[Platform health check] -->|GET /healthz| Bot
    Monitor[Keepalive / uptime monitor] -->|GET /readyz| Bot
    Bot --> Flow[GameFlow<br/>prompt, timeout, chat lock]
    Flow --> Classic[ClassicGameService]
    Flow --> Sheldon[SheldonGameService]
    Flow --> Messages[Messages<br/>render RoundResult]
```

The bot is an outbound-only client of the Telegram Bot API. It does not expose a
webhook, so it can run behind NAT or on a PaaS without a public URL. It is deployed
to Render as a Docker web service; the Ktor server binds `0.0.0.0:$PORT` (Render
provides `PORT`, default 8080) and serves only `GET /healthz` (liveness) and
`GET /readyz` (Telegram connectivity).

## Components

| Component | File | Responsibility |
| --- | --- | --- |
| Entry point | `src/main/kotlin/com/el_giancar/Application.kt` | Composition root: reads `AppConfig`, starts `embeddedServer(Netty)`, installs `/healthz` and `/readyz`, wires services, then runs the Telegram loop on the main thread. |
| Config | `src/main/kotlin/com/el_giancar/config/AppConfig.kt` | Reads `BOT_TOKEN` and `PORT`; fails fast when the token is missing. |
| Telegram adapter | `src/main/kotlin/com/el_giancar/infrastructure/telegram/TelegramBot.kt` | Creates the bot, registers commands, runs long polling, and refreshes readiness with a 60-second `getMe()` monitor. |
| Game flow | `src/main/kotlin/com/el_giancar/infrastructure/telegram/GameFlow.kt` | Acquires the per-chat lock, prompts for a move, applies the two-minute timeout, and renders the result. Telegram I/O goes through `GameIo`. |
| Game I/O | `src/main/kotlin/com/el_giancar/infrastructure/telegram/GameIo.kt` | `GameIo` send/await seam plus the real `TelegramGameIo`, so the flow is testable without a bot. |
| Move parser | `src/main/kotlin/com/el_giancar/infrastructure/telegram/MoveParser.kt` | Maps trimmed, case-insensitive text to a legal move of the selected game. |
| Keyboards / messages | `src/main/kotlin/com/el_giancar/infrastructure/telegram/Keyboards.kt`, `Messages.kt`, `Replies.kt` | Reply keyboards, user-facing text, and shared reply helpers. |
| HTTP adapter | `src/main/kotlin/com/el_giancar/infrastructure/http/HealthRoutes.kt` | `GET /healthz` (always 200) and `GET /readyz` (200/503 from the readiness state). |
| Move domain | `src/main/kotlin/com/el_giancar/domain/Move.kt`, `ClassicMoveType.kt`, `SheldonMoveType.kt` | Legal moves, their display names, and the two variants. |
| Round domain | `src/main/kotlin/com/el_giancar/domain/GameOutcome.kt`, `RoundResult.kt` | Typed outcome (`WIN`/`LOSE`/`TIE`) and the resolved round data. |
| Game application | `src/main/kotlin/com/el_giancar/application/Game.kt`, `GameRules.kt`, `GameService.kt` | Variant contract, legal moves + winning phrases, and shared round resolution. Stateless and Telegram-free. |
| Game variants | `src/main/kotlin/com/el_giancar/application/ClassicGameService.kt`, `SheldonGameService.kt` | The 3-move and 5-move rule sets; thin subclasses of `GameService`. |
| Chat locks | `src/main/kotlin/com/el_giancar/application/ChatLocks.kt` | Reference-counted per-chat `Mutex`; idle entries are evicted after release. Keeps the application layer Telegram-free. |
| Readiness | `src/main/kotlin/com/el_giancar/application/ReadinessState.kt` | Thread-safe connectivity flag shared by the Telegram adapter and `/readyz`. |
| Tests | `src/test/kotlin/com/el_giancar/{application,config}/*Test.kt`, `infrastructure/{http,telegram}/` | Exhaustive pair tables, canonical phrases, message formatting, move parsing, environment parsing, health/readiness routes, log redaction, chat-lock eviction, and the full `GameFlow` prompt loop through a fake `GameIo`. |
| Logging | `src/main/resources/logback.xml`, `infrastructure/logging/JulBridge.kt` | Console appender at `info`, token redaction, and a JUL-to-SLF4J bridge so tgbotapi logs use the same pattern. |
| API spec | `src/main/resources/openapi/documentation.yaml` | Description of `/healthz` and `/readyz`; not referenced by code. |
| CI | `.github/workflows/ci.yml` | Builds and tests on push/PR. |
| Deploy | `.github/workflows/deploy-render.yml` | Updates `BOT_TOKEN` on Render and triggers a deploy. |

## Startup sequence

```mermaid
sequenceDiagram
    participant Main as main()
    participant Env as Environment
    participant Ktor as Ktor/Netty
    participant TG as runTelegramBot()
    participant API as Telegram Bot API

    Main->>Env: AppConfig.fromEnvironment() (fail fast if BOT_TOKEN is missing)
    Main->>Ktor: embeddedServer(..., port)
    Main->>Ktor: start(wait = false)
    Ktor->>Ktor: routing { GET /healthz, GET /readyz }
    Main->>TG: runBlocking { runTelegramBot(token, games, locks, readiness) }
    TG->>API: telegramBot(token), getMe()
    TG->>TG: readiness.markReady()
    TG->>API: buildBehaviourWithLongPolling (getUpdates loop)
    TG->>API: getMe() every 60s (readiness monitor)
    Note over TG,API: join() keeps the coroutine alive
    Note over Main,Ktor: finally -> readiness.markNotReady(); server.stop(...)
```

The HTTP server starts **before** the Telegram loop, so `/healthz` and `/readyz`
are reachable while the bot is starting. `/healthz` stays 200 regardless of Telegram;
`/readyz` returns 503 until the first successful `getMe()` and again if the periodic
connection check fails. `runBlocking` keeps the main thread on the long-polling
task; if Telegram startup or polling fails, the exception propagates, the server is
stopped in `finally`, and the process exits non-zero. Ktor uses a `SupervisorJob`
for application children, which is why the bot is not launched as a fire-and-forget
child.

## Update flow

```mermaid
sequenceDiagram
    participant U as User
    participant TG as Telegram Bot API
    participant B as GameFlow
    participant L as Per-chat mutex
    participant S as Game service
    participant M as Messages

    U->>TG: /sheldon_game
    TG-->>B: update (long polling)
    B->>L: tryLock(chatId)
    alt chat is busy
        B->>TG: "A game is already in progress..."
    else lock acquired
        B->>TG: "Your move!" + reply keyboard
        U->>TG: spock (lowercase is accepted)
        TG-->>B: text update (same chat)
        B->>B: trim + case-insensitive match
        B->>S: play(SPOCK)
        S->>S: random move + winning phrase lookup
        S-->>B: RoundResult(playerMove, botMove, outcome, rulePhrase)
        B->>M: resultMessage(result)
        M-->>B: "You: Spock\nMe: Rock\n\n🎉 You win! Spock vaporizes Rock."
        B->>TG: result + commands keyboard
        B->>L: unlock(chatId)
    end
```

Key points:

- Command handlers are coroutines owned by tgbotapi's behaviour builder.
- `onCommand` runs the scenario in a subcontext filtered by `MessageFilterByChat`,
  so updates are already scoped to the originating chat. The game code adds an
  explicit `it.chat.id == message.chat.id` check as a second safety net.
- A `ConcurrentHashMap<Long, Mutex>` serializes games per chat. Different chats run
  independently.
- `GameFlow` wraps the prompt loop in `withTimeoutOrNull(2.minutes)`. A game that
  receives no move releases the lock and tells the user it timed out.
- Input is trimmed and matched case-insensitively against the moves' display names.
  Invalid input gets a hint and up to three tries; any command cancels the game. No
  state is persisted.

## Domain model

Every move implements `Move`, which supplies a human-readable `displayName`:

```kotlin
interface Move {
    val displayName: String
}

enum class ClassicMoveType(override val displayName: String) : Move {
    ROCK("Rock"), PAPER("Paper"), SCISSORS("Scissors")
}

enum class SheldonMoveType(override val displayName: String) : Move {
    ROCK("Rock"), PAPER("Paper"), SCISSORS("Scissors"),
    LIZARD("Lizard"), SPOCK("Spock")
}
```

A resolved round is data, not prose:

```kotlin
data class RoundResult<out M : Move>(
    val playerMove: M,
    val botMove: M,
    val outcome: GameOutcome,   // WIN, LOSE, TIE
    val rulePhrase: String?,    // e.g. "Rock crushes Scissors"; null for a tie
)
```

The Telegram layer parses text (case-insensitive, by `displayName`) into an enum
value; the services only accept enums. Reply buttons use `displayName`, so users see
`Rock` rather than `ROCK`.

### Winner resolution

`GameRules<M>` owns the legal moves and a `phrases` map keyed by `(winner, loser)`:

- `ClassicRules`: 3 pairs.
- `SheldonRules`: 10 pairs.

`GameService<M>` resolves a round by looking up `(player, bot)` and `(bot, player)`:

- equal moves → `TIE`, no phrase;
- `(player, bot)` present → `WIN` with that phrase;
- `(bot, player)` present → `LOSE` with that phrase;
- anything else → `error(...)`, which indicates a missing rule.

`ClassicGameService` and `SheldonGameService` only bind the generic service to their
rule set. Each service also exposes a deterministic `play(playerMove, botMove)`
overload. This keeps tests free of randomness. The random overload picks with
`java.util.Random` (injectable via the constructor).

## Error handling

- Missing `BOT_TOKEN` fails at startup with an actionable message.
- Unknown moves are re-prompted (max 3 attempts), then the game is cancelled cleanly.
- Sending a command while a game is pending cancels the game and releases the lock.
- A game with no move for two minutes times out, tells the user, and releases the lock.
- A second game in the same chat is rejected without disturbing the first.
- Telegram/network failures still propagate to the handler coroutine; there is no
  retry/backoff layer.

## Deployment

The bot is deployed to **Render** as a Docker web service. GitHub Actions drives the
release: it pushes the current `BOT_TOKEN` secret to Render, then triggers a deploy.
Details and the one-time setup are in [`DEPLOYMENT.md`](DEPLOYMENT.md).

```mermaid
flowchart TB
    Dev[git push / v* tag / manual dispatch] --> GHA[GitHub Actions<br/>deploy-render.yml]
    GHA -->|PUT env-vars/BOT_TOKEN| RenderAPI[Render API]
    GHA -->|POST deploys| RenderAPI
    RenderAPI --> Render[Render Docker web service]
    Render --> Build[Build stage: gradle buildFatJar]
    Build --> Runtime[Runtime: java -jar /app/app.jar]
    Runtime --> Health[Ktor 0.0.0.0:$PORT<br/>GET /healthz + /readyz]
    Runtime --> Poll[Telegram long polling]
    Secret[GitHub secret BOT_TOKEN] --> GHA
    Secret -.->|stored as env var| Runtime
```

Notes:

- The Dockerfile copies `build/libs/*-all.jar` to a stable `/app/app.jar`, avoiding
  coupling to the project version in the file name.
- The token is injected at runtime as the Render environment variable `BOT_TOKEN`; it
  is never a build argument and is never baked into the image.
- CI (`.github/workflows/ci.yml`) builds and tests every push/PR. The deploy workflow
  (`.github/workflows/deploy-render.yml`) runs on `v*` tags or manual dispatch.
- Build and runtime both use JDK 21. Render supplies `PORT` and probes `/healthz`.
  The keepalive workflow probes `/readyz` so a disconnected bot is visible.
- The bot only needs a long-running process and outbound HTTPS to `api.telegram.org`;
  this architecture works on any comparable PaaS.

## Design decisions

- **Ktor with liveness and readiness routes.** `/healthz` keeps the process alive
  for PaaS checks without restarting it on a Telegram outage; `/readyz` reports the
  Telegram connection so monitors can tell a broken bot from a sleeping service.
- **Services separated from Telegram.** `application` has no tgbotapi dependency, which
  makes the rules unit-testable.
- **Typed service API.** Enums instead of strings remove an entire class of parsing
  bugs (case, unknown moves, wrong enum set).
- **Typed results, presentation in the adapter.** Services return `RoundResult`;
  `Messages.kt` turns it into text. Game rules stay free of wording and the bot can
  change its voice without touching the rules.
- **One shared round resolver.** `GameService` implements win/lose/tie once; the two
  variants only supply a rule set.
- **Long polling over webhooks.** Simpler ops, no TLS termination or public URL.
- **No shared game state.** Rounds are independent; only the per-chat mutex map is
  in memory. Adding scores would require a store and a per-chat/user key.

## Extension points

| Goal | Where to change |
| --- | --- |
| New command | `infrastructure/telegram/TelegramBot.kt` (`onCommand`) + `Keyboards.kt` + `Messages.kt` |
| New move | relevant `domain/*MoveType.kt` (enum + `displayName`), the variant's `phrases` map, `/rules`, tests, docs |
| Change the bot's wording | `infrastructure/telegram/Messages.kt` only |
| Change the game timeout | `GameFlow`'s `gameTimeout` parameter |
| Better randomness / tests | pass a seeded `Random` to the service constructor, or keep using the deterministic overload |
| Scores / persistence | Introduce a repository keyed by `chat.id`/`user.id`; keep it out of the stateless services |
| Serve the OpenAPI spec | Add a route that reads `openapi/documentation.yaml`, or use Ktor's OpenAPI plugin |
| Webhook mode | Replace `buildBehaviourWithLongPolling` with the webhook builder and add a Ktor route |

## Non-goals

The current code does not aim to provide accounts, leaderboards, localization,
rate limiting, metrics, or horizontal scaling. Any of these would require new
infrastructure and should be designed explicitly.
