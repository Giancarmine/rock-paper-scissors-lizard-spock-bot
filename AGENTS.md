# AGENTS.md

Guidance for AI coding agents (OpenCode, GitHub Copilot, Cursor, Claude Code, etc.)
working in this repository. Read this before changing code.

## Project snapshot

- **What it is:** a Telegram chat bot that plays _Rock–Paper–Scissors_ and
  _Rock–Paper–Scissors–Lizard–Spock_ against the user. The bot picks a random move
  and replies with a human-readable result.
- **Language/runtime:** Kotlin/JVM 2.4.21 on Java 21 (used for both build and
  runtime images).
- **Frameworks:** Ktor 3.6.1 (Netty server), [`dev.inmo:tgbotapi`](https://github.com/InsanusMokrassar/TelegramBotAPI) 37.1.0
  for Telegram, Logback 1.6.6 for logging.
- **Build:** Gradle 9.8.2 via the committed wrapper (`./gradlew`), with dependency
  versions in `gradle/libs.versions.toml`. ktlint 14.2.0 enforces Kotlin formatting.
- **Size:** 33 Kotlin source files, 16 test files, GitHub Actions CI, optional
  PostgreSQL score persistence.
- **Deployment:** single Docker image on **Render** (Docker web service, health check
  `/healthz`). The bot uses Telegram **long polling**, so no webhook is required.
  `.github/workflows/deploy-render.yml` pushes `secrets.BOT_TOKEN` to Render, copies
  optional `DATABASE_*` secrets, and triggers a deploy. The service stays on the free
  plan, and `.github/workflows/keepalive.yml` pings `/readyz` every 5 minutes so Render
  does not sleep and stop polling.

Repository root is the directory containing this file.

## Secrets and configuration

The bot reads its token from the **`BOT_TOKEN` environment variable** and fails fast
when it is missing. `PORT` optionally overrides the HTTP port (default `8080`).
`DATABASE_URL` optionally enables score tracking; `DATABASE_USER` and
`DATABASE_PASSWORD` are optional overrides for URLs without embedded credentials.

```bash
export BOT_TOKEN="<your-token>"
export PORT=8080            # optional
export DATABASE_URL="postgresql://user:password@host/db?sslmode=require"  # optional
```

Rules for agents:

1. **Never** print, copy, log, echo, or commit real secrets. Use `BOT_TOKEN` /
   `<your-token>` placeholders in docs, tests, fixtures, and commit messages. The same
   rule applies to `DATABASE_URL`, `DATABASE_USER`, and `DATABASE_PASSWORD`.
2. `.env` is git-ignored; `.env.example` documents the variable names.
3. tgbotapi includes the Telegram request URL (and therefore the token) in error
   messages. `src/main/resources/logback.xml` redacts `bot<id>:<secret>` from every
   message and stack trace; do not remove that pattern, and add a test with a fake
   token if you change it. tgbotapi logs through kslog, which uses
   `java.util.logging` directly on the JVM; `installJulToSlf4jBridge()` in
   `Application.kt` routes those records through Logback. Do not remove it — without
   the bridge, JUL records bypass the redaction pattern and the token is logged.
4. If a token is ever exposed, rotate it via [@BotFather](https://t.me/BotFather) and
   update the `BOT_TOKEN` secret. Do not rewrite git history unless the owner
   explicitly asks.
5. Do not add tokens or database credentials to `Dockerfile`, `build.gradle.kts`,
   source files, or workflow files.
6. Production tokens live in GitHub Actions secrets: `BOT_TOKEN`,
   `RENDER_API_KEY`, plus the `RENDER_SERVICE_ID` repository variable. Database
   credentials, when used, live in the optional `DATABASE_URL`,
   `DATABASE_USER`, and `DATABASE_PASSWORD` secrets. The deploy workflow copies
   configured values to Render at deploy time and never echoes them.
7. `PostgresStatsStore` moves `user:password@` out of the JDBC URL before creating the
   pool, and startup logs only the exception type if initialization fails. Do not log
   the raw URL or Hikari configuration; both can contain credentials.

## Quick start

Prerequisites: JDK 21 and network access for Gradle dependencies. The current
sources were verified with Temurin JDK 21 (`./gradlew clean test buildFatJar`).

```bash
export BOT_TOKEN="<your-token>"

./gradlew build          # compile + assemble + run tests
./gradlew test           # unit tests only
./gradlew ktlintCheck    # Kotlin formatting check
./gradlew ktlintFormat   # fix formatting automatically
./gradlew run            # starts the bot (long polling) and Ktor on :8080
./gradlew buildFatJar    # build/libs/rock-paper-scissors-lizard-spock-bot-all.jar
java -jar build/libs/rock-paper-scissors-lizard-spock-bot-all.jar

curl http://localhost:8080/healthz      # -> OK
curl http://localhost:8080/readyz       # -> READY once Telegram is connected
curl http://localhost:8080/openapi.yaml # -> OpenAPI 3.0 document
```

Docker:

```bash
docker build -t rpsls-bot .
docker run --rm -p 8080:8080 -e BOT_TOKEN="<your-token>" rpsls-bot
```

Notes:

- Ktor binds `0.0.0.0:$PORT` (default 8080) and serves only `/healthz`,
  `/readyz`, and `/openapi.yaml`. All real functionality goes through Telegram long
  polling.
- The bot does nothing useful without a valid token and outbound HTTPS access to
  `api.telegram.org`.

## Architecture at a glance

```text
Application.kt                        main(): reads config, starts Ktor
  │                                   (GET /healthz, /readyz, /openapi.yaml), then runBlocking { runTelegramBot }
  ├─ config/AppConfig.kt              BOT_TOKEN / PORT / optional DATABASE_* from the environment
  ├─ infrastructure/http/             liveness, readiness, and OpenAPI routes
  ├─ infrastructure/logging/          JulBridge.kt + ExpectedLongPollTimeoutFilter.kt
  ├─ infrastructure/stats/            PostgresStatsStore.kt: HikariCP + schema + queries
  └─ infrastructure/telegram/         TelegramBot.kt: command wiring + long polling + readiness monitor
       ├─ GameFlow.kt                 prompt loop, timeout, per-chat lock, returns RoundResult?
       ├─ GameIo.kt                   send/await seam (real + fake implementations)
       ├─ InlineResults.kt            inline-mode Play/Share cards
       ├─ Milestones.kt               new-leader and point milestone detection
       ├─ MoveParser.kt               text -> move (case-insensitive)
       ├─ Players.kt                  Telegram user -> Player
       ├─ Messages.kt / Keyboards.kt  user-facing text and keyboards
       ├─ domain/*.kt                 Move, move enums, GameOutcome, RoundResult, stats types
       └─ application/*.kt            Game, GameRules, GameService, StatsService and the two variants
```

| Path | Responsibility |
| --- | --- |
| `src/main/kotlin/com/el_giancar/Application.kt` | Composition root: reads config, starts Ktor with `/healthz`, `/readyz`, and `/openapi.yaml`, wires services, then runs Telegram on the main thread. |
| `src/main/kotlin/com/el_giancar/config/AppConfig.kt` | Reads `BOT_TOKEN`, `PORT`, and optional database settings; fails fast when the token is missing. |
| `src/main/kotlin/com/el_giancar/domain/Move.kt` | Move contract: human-readable `displayName`. |
| `src/main/kotlin/com/el_giancar/domain/GameOutcome.kt` | `WIN` / `LOSE` / `TIE` from the player's point of view. |
| `src/main/kotlin/com/el_giancar/domain/RoundResult.kt` | Typed result: both moves, outcome, and optional rule phrase. |
| `src/main/kotlin/com/el_giancar/domain/Player.kt` | Telegram player identity and display name. |
| `src/main/kotlin/com/el_giancar/domain/PlayerStats.kt` | Per-chat and overall aggregates plus `PersonalStats`. |
| `src/main/kotlin/com/el_giancar/domain/LeaderboardEntry.kt` | Ranked leaderboard row. |
| `src/main/kotlin/com/el_giancar/domain/GameRecord.kt` | A completed round that should be persisted. |
| `src/main/kotlin/com/el_giancar/domain/ClassicMoveType.kt` | `ROCK/PAPER/SCISSORS` enum with display names. |
| `src/main/kotlin/com/el_giancar/domain/SheldonMoveType.kt` | `ROCK/PAPER/SCISSORS/LIZARD/SPOCK` enum with display names. |
| `src/main/kotlin/com/el_giancar/application/Game.kt` | Playable-game contract used by the Telegram layer. |
| `src/main/kotlin/com/el_giancar/application/GameRules.kt` | Legal moves and winning-phrase lookup for one variant. |
| `src/main/kotlin/com/el_giancar/application/GameService.kt` | Shared random move + win/lose/tie resolution; returns `RoundResult`. |
| `src/main/kotlin/com/el_giancar/application/ClassicGameService.kt` | Binds `GameService` to the classic rule set. |
| `src/main/kotlin/com/el_giancar/application/SheldonGameService.kt` | Binds `GameService` to the 5-move rule set. |
| `src/main/kotlin/com/el_giancar/application/ChatLocks.kt` | Reference-counted per-chat `Mutex`; entries are evicted when the last caller releases an unlocked lock. |
| `src/main/kotlin/com/el_giancar/application/StatsStore.kt` | Persistence boundary for completed games, personal stats, and leaderboards. |
| `src/main/kotlin/com/el_giancar/application/StatsService.kt` | Optional scores; maps a missing/unreachable store to `Disabled`/`Unavailable` and never throws into gameplay. |
| `src/main/kotlin/com/el_giancar/application/ReadinessState.kt` | Thread-safe flag for Telegram connectivity, updated by the bot and read by `/readyz`. |
| `src/main/kotlin/com/el_giancar/infrastructure/http/HealthRoutes.kt` | `GET /healthz` (liveness) and `GET /readyz` (Telegram readiness) routes. |
| `src/main/kotlin/com/el_giancar/infrastructure/http/OpenApiRoutes.kt` | `GET /openapi.yaml`; reads the bundled spec once at startup and serves it as `application/yaml`. |
| `src/main/kotlin/com/el_giancar/infrastructure/logging/ExpectedLongPollTimeoutFilter.kt` | Logback turbo filter that drops only the expected idle `getUpdates` request timeout. |
| `src/main/kotlin/com/el_giancar/infrastructure/stats/PostgresStatsStore.kt` | HikariCP pool, URL credential extraction, retrying schema bootstrap, writes, personal stats, and leaderboards. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/TelegramBot.kt` | Bot wiring: commands, long polling, result recording, group onboarding, milestones, inline queries, `/start` attribution, and the 60-second readiness monitor. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/BotCommands.kt` | The `/` command menu, published at startup so it always matches the deployed code. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/BotLinks.kt` | Public bot URL and the share deep link used by the result keyboard. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/GameFlow.kt` | Prompt loop, 2-minute timeout, per-chat lock, result rendering, and the completed `RoundResult` (or `null`). |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/GroupOnboarding.kt` | Detects the transition that means the bot was added to a group. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/InlineResults.kt` | Builds the inline-mode article results and their Play/Share buttons. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Milestones.kt` | Pure new-leader and point-milestone detection from before/after leaderboards. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/StartPayload.kt` | Parses and sanitizes `/start` deep-link payloads. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/GameIo.kt` | `GameIo` seam (send/await text) with the real `TelegramGameIo` implementation, so the flow is unit-testable. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/MoveParser.kt` | Case-insensitive text-to-move parsing. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Players.kt` | Telegram `CommonUser` to `Player`, with display-name fallbacks. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Replies.kt` | Shared reply/menu helpers. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Keyboards.kt` | Reply keyboards (move buttons use display names). |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Messages.kt` | All user-facing text, `RoundResult` formatting, and stats/leaderboard messages. |
| `src/test/kotlin/com/el_giancar/application/*Test.kt` | Exhaustive pair tables, typed results, chat-lock eviction, and stats fallback behavior. |
| `src/test/kotlin/com/el_giancar/config/AppConfigTest.kt` | Environment parsing and fail-fast behavior. |
| `src/test/kotlin/com/el_giancar/infrastructure/http/HealthRouteTest.kt` | Ktor tests for `GET /healthz` and `GET /readyz` (ready and not ready). |
| `src/test/kotlin/com/el_giancar/infrastructure/http/OpenApiRouteTest.kt` | Verifies `GET /openapi.yaml` status, content type, bundled body, and injected document. |
| `src/test/kotlin/com/el_giancar/infrastructure/logging/ExpectedLongPollTimeoutFilterTest.kt` | Verifies that only `getUpdates` timeouts are dropped, including wrapped exceptions. |
| `src/test/kotlin/com/el_giancar/infrastructure/logging/LogbackConfigTest.kt` | Verifies that bot tokens are redacted from messages and stack traces. |
| `src/test/kotlin/com/el_giancar/infrastructure/logging/JulBridgeTest.kt` | Verifies that `java.util.logging` records reach Logback and are redacted. |
| `src/test/kotlin/com/el_giancar/infrastructure/stats/PostgresStatsStoreTest.kt` | H2-backed repository, leaderboard ranking, display-name updates, and PostgreSQL URL conversion. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/GameFlowTest.kt` | Prompt, retry, cancellation, timeout and busy-chat flow tests through a fake `GameIo`, including the returned result and share keyboard. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/IdleTextTest.kt` | Verifies the idle-text hint predicate: commands, blank text, busy chats and group chats are excluded. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/InlineResultsTest.kt` | Inline-mode query filtering, result text, and Play/Share buttons. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/MilestonesTest.kt` | New-leader and point-milestone detection, including priority and zero-point guards. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/MoveParserTest.kt` | Case-insensitive parsing and per-game move validation. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/PlayersTest.kt` | Display-name fallbacks and truncation. |
| `src/test/kotlin/com/el_giancar/infrastructure/telegram/MessagesTest.kt` | Exact win/tie wording, both moves, prompt contents, milestone text, and stats/leaderboard formatting. |
| `src/main/resources/logback.xml` | Console logging at `info`, bot-token redaction, and the idle-timeout filter. |
| `src/main/resources/openapi/documentation.yaml` | Documents `/healthz`, `/readyz`, and `/openapi.yaml`; served by `OpenApiRoutes.kt`. |
| `Dockerfile`, `.dockerignore` | Two-stage JDK 21 build, fat jar runtime. |
| `.github/workflows/ci.yml` | CI: `./gradlew ktlintCheck` and `./gradlew build buildFatJar` on Temurin 21. |
| `.github/workflows/deploy-render.yml` | Updates `BOT_TOKEN` and optional `DATABASE_*` variables on Render, then triggers a deploy. |
| `.github/workflows/keepalive.yml` | Pings `/readyz` every 5 minutes so the free Render service stays awake, and fails if the Telegram connection is dead. |
| `docs/DEPLOYMENT.md` | Render setup, secrets, database setup, and token-rotation runbook. |
| `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md` | Contribution workflow, private vulnerability reporting, and release notes. |
| `.github/dependabot.yml`, issue/PR templates | Dependency updates and contribution scaffolding. |
| `build.gradle.kts`, `gradle/libs.versions.toml` | Build logic and dependency versions. |

### Telegram update flow

1. `main()` builds `AppConfig` from `BOT_TOKEN` (failing fast if absent), `PORT`, and
   optional `DATABASE_*`, creates the stats service, starts Ktor with `/healthz`,
   `/readyz`, and `/openapi.yaml`, then calls `runTelegramBot(...)`, which builds the
   bot, both games, a stats service, and a readiness monitor, then starts
   `buildBehaviourWithLongPolling`.
2. `onCommand("classic_game" | "sheldon_game")` calls `GameFlow.play(...)`, which
   acquires a **per-chat mutex** so two games cannot run in the same chat at once.
   If the chat is busy, the user is told to finish the current game.
3. `GameFlow` sends a prompt with a one-time reply keyboard and waits for a text
   message **from the same chat and the player who started the game** through `GameIo`
   (the real implementation uses `waitTextMessage().first { ... }`), wrapped in a
   two-minute `withTimeoutOrNull`.
4. `MoveParser` trims the text and matches it case-insensitively against each move's
   `displayName`. Invalid input gets a hint and up to `maxMoveAttempts` (3) retries;
   any command cancels the game. A timeout releases the lock with a clear message.
5. The game service resolves the round and returns a typed `RoundResult`.
6. `Messages.resultMessage(...)` renders both moves and the outcome; the bot sends it
   with the inline share keyboard in a single message (a message cannot carry both a
   reply keyboard and an inline keyboard).
7. `GameFlow.play` returns the `RoundResult` for a completed round, or `null` for a
   busy chat, timeout, cancellation, or too many invalid attempts. Only non-null
   results are recorded through `StatsService`, after the reply is sent. In a group,
   the bot reads the player's chat points and the chat leaderboard before and after the
   write and announces at most one milestone: a new leader or an exact point total.
8. `/start`, `/rules`, `/help`, `/about`, `/commands`, `/stats`, `/leaderboard`, and
   `/chat_leaderboard` reply directly; `/start` also reads a deep-link payload, logs a
   sanitized campaign token, and uses the share-specific greeting for `?start=share`.
   `/cancel` reports that no game is running when the chat is idle (during a game the
   `GameFlow` prompt already consumes it as a cancellation); unknown commands get a
   hint via `onUnhandledCommand`.
9. `onText` gives an idle private chat a hint when it receives plain text. It stays
   silent for commands, for chats where a game holds the lock, and for group chats.
10. `onMyChatMemberUpdated` posts the group welcome when the bot transitions from
    `Left`/`Kicked` to `Member`/`Administrator`/`Creator` in a group. Promotions and
    private-chat membership changes are ignored.
11. `onAnyInlineQuery` answers with one or two `InlineQueryResultArticle`s. The query
    only selects a variant (`classic`, or `lizard`/`spock`/`sheldon`); the cards always
    link to the same bot and share URLs. Telegram sends inline queries only after the
    owner enables inline mode in BotFather with `/setinline`.

Game state is still in memory: the only shared live state is the per-chat mutex map.
Completed rounds are the exception — they are written to PostgreSQL when
`DATABASE_URL` is configured. With no database, scores are disabled; with an
unreachable database, the stats commands report that tracking is unavailable. A
database that is asleep or unreachable at startup is retried on the first game or
stats request, so a transient outage does not require a restart. Neither case blocks a
game.

## Game rules (source of truth: the rule sets)

| Move | Beats |
| --- | --- |
| ROCK | SCISSORS, LIZARD |
| PAPER | ROCK, SPOCK |
| SCISSORS | PAPER, LIZARD |
| LIZARD | PAPER, SPOCK |
| SPOCK | ROCK, SCISSORS |

The classic variant uses only ROCK/PAPER/SCISSORS. Ties are equal moves. The private
`ClassicRules` / `SheldonRules` objects hold a `phrases` map keyed by
`(winner, loser)`. `GameService` returns a `RoundResult`; the Telegram layer renders
text such as `"🎉 You win! Spock vaporizes Rock."` or `"🤝 It's a tie!"`.

When adding a winning relation, update the variant's `phrases` map once — losses are
derived by looking up the pair in the opposite order.

## Conventions

- Package layout is `com.el_giancar.{config,domain,application,infrastructure}`; put
  environment/config in `config`, pure types in `domain`, game rules in `application`,
  and Ktor/Telegram adapters in `infrastructure`.
- Keep services **stateless** and free of tgbotapi imports so they stay unit-testable.
- Services accept **enum values**, not raw strings. Parsing/validation belongs in the
  Telegram layer (`MoveParser.kt`).
- Services return a typed `RoundResult`; they never build user-facing strings.
  Put all wording in `infrastructure/telegram/Messages.kt` so the bot's voice stays
  in one place.
- Keep persistence behind `StatsStore` (`application`) and implement it in
  `infrastructure/stats`. Never let a database error escape into `GameFlow` or a game
  service; `StatsService` converts failures to `StatsResult.Disabled`/`Unavailable`.
- Never log `DATABASE_URL` or Hikari configuration. The store strips embedded
  credentials before building the JDBC URL, and startup logs only the exception type.
- Keep schema initialization idempotent and retryable: `PostgresStatsStore.initialize()`
  is safe to call repeatedly. Do not replace a failed startup initialization with a
  permanently `null` store; the next operation must be able to retry.
- Each service exposes two overloads: `play(move)` (random opponent) and
  `play(playerMove, botMove)` (deterministic, used by tests).
- Move enums use `UPPER_SNAKE_CASE` and set a human-readable `displayName`; reply
  buttons use `displayName`.
- Kotlin official style, 4-space indentation, LF line endings (see `.editorconfig`).
  Run `./gradlew ktlintCheck` before committing; `.editorconfig` disables only
  `standard:package-name` because the package name intentionally contains an
  underscore.
- Keep dependency versions in `gradle/libs.versions.toml`; avoid hardcoding versions in
  `build.gradle.kts`.
- Prefer additive, focused changes. Do not reformat or rename unrelated code in a
  bug-fix change.
- Add new dependencies only when necessary; this project is intentionally small.

## Known pitfalls

- Games time out after **two minutes** (`GameFlow.gameTimeout`). The lock is released
  in a `finally` block via `ChatLock.release()`, which also drops the pin, so a
  timed-out game cannot stay locked or leak a map entry. If you change the timeout,
  keep that structure and always call `release()` exactly once per `acquire()`.
- `waitTextMessage()` expects the update flow of the command's subcontext. Command
  handlers already filter by chat (`MessageFilterByChat`); the explicit chat-id check
  is a second safety net. Do not remove either without evaluating concurrency.
- `TelegramGameIo.awaitText()` accepts text only from the chat **and** the user who
  started the game (`isMoveFromOriginatingPlayer`). In a group, other members' moves
  and commands are ignored so a round cannot be played or credited to the wrong
  player. Keep the helper and `GameIoTest` in sync if you change the filter.
- tgbotapi broadcasts each update to every trigger and to `waitTextMessage()`. During
  a game, `/cancel` reaches both the registered handler and the prompt loop, so the
  handler deliberately stays silent while `ChatLocks.isBusy(chatId)` is true; the
  prompt loop sends the single cancellation reply. Without that guard the user gets
  two messages.
- The same broadcast applies to `onText`. It must skip commands (handled by
  `onCommand`/`onUnhandledCommand`), skip chats where a game holds the lock (the
  prompt loop owns that text), and skip group chats so ordinary conversation is not
  interrupted. `shouldHintOnIdleText` encodes those rules; keep it covered by
  `IdleTextTest`.
- `ChatIdentifier`/`ChatId` is a value class; the lock map is keyed by the underlying
  `Long` (`message.chat.id.chatId.long`).
- Input is matched against `displayName`, not the enum `name`. Adding a move without
  setting `displayName` (and without listing it in `/rules`) makes it invisible in
  the UI even though the rules would accept it.
- The OpenAPI YAML is served at `GET /openapi.yaml` by `OpenApiRoutes.kt`; update
  the resource when the HTTP surface changes.
- tgbotapi 37 sets the HTTP request timeout equal to Telegram's long-poll timeout,
  so an idle bot raises an `HttpRequestTimeoutException` roughly every 30 seconds.
  The polling loop recovers on the next request and updates are not lost.
  `ExpectedLongPollTimeoutFilter` drops only those `getUpdates` timeouts; do not widen
  it to all timeouts. Do not remove the log redaction pattern or the jul-to-slf4j
  bridge because that exception message contains the token.
- Render's free plan sleeps after 15 minutes without inbound traffic. The scheduled
  `keepalive.yml` workflow pings `/readyz`; keep it enabled while on the free plan.
  `/healthz` must stay a liveness probe (always 200): pointing it at Telegram state
  would make the platform restart a process that is merely disconnected.
- `getNames<T>()` was removed together with string-based parsing. Do not reintroduce
  it unless it has a real caller.
- Never commit a token. If one is exposed, rotate it via @BotFather, update the
  `BOT_TOKEN` secret, and redeploy; the redaction pattern and JUL bridge protect new
  logs but cannot scrub old ones.

## Common tasks

### Add a Telegram command

1. Register it in `src/main/kotlin/com/el_giancar/infrastructure/telegram/TelegramBot.kt`
   with `onCommand("name") { message -> ... }`.
2. Add it to `botCommands` in `BotCommands.kt` so it appears in Telegram's `/` menu.
3. Add a `simpleButton("/name")` to `commandsKeyboard()` in `Keyboards.kt` if it should
   be discoverable.
4. Add it to `helpMessage` in `Messages.kt` and to `README.md`.
5. Keep the reply style consistent with the existing messages: friendly, concise,
   emoji-led where useful, and plain text (no Markdown parse mode).

### Add or change a move / rule

1. Add the enum constant and its `displayName` to the relevant `domain/*MoveType.kt`.
2. Add/update the pair in the matching rules object's `phrases` map. A move must beat
   exactly two others in the five-move game.
3. Update the `/rules` text in `infrastructure/telegram/Messages.kt`.
4. Update the rules table in this file, `README.md`, and `docs/ARCHITECTURE.md`.
5. Extend the exhaustive test table in `src/test/kotlin/...` so all pairs stay
   covered.

### Change the bot's wording

Edit `src/main/kotlin/com/el_giancar/infrastructure/telegram/Messages.kt` only. Do not
move user-facing text into the game services. `MessagesTest` asserts the exact result
format, so update it together with the wording.

### Add tests

Tests live in `src/test/kotlin`. Use `kotlin.test` on the JUnit Platform (Jupiter 6;
`kotlin-test-junit5` is already declared). Prefer the deterministic `play(player, bot)`
overload; no random stubbing is required. Add message/parser tests under
`src/test/kotlin/com/el_giancar/infrastructure/telegram/`. Run with `./gradlew test`.

### Change configuration

Read environment variables in `AppConfig.fromEnvironment()`, fail fast with a clear
message, and update `.env.example`, `README.md`, and `AGENTS.md`. Never add a default
token.

### Rotate the bot token

1. Revoke the old token and create a new one with @BotFather (owner action).
2. Store it as the GitHub secret: `gh secret set BOT_TOKEN`.
3. Run the deploy workflow: `gh workflow run deploy-render.yml`. It updates the
   Render environment variable and redeploys.
4. Confirm the deploy is live (`render deploys list <srv-id>`), `/healthz` returns
   `OK`, `/readyz` returns `READY`, then send `/start` to the bot.
5. Confirm Render logs show `bot<redacted>` and not the token.

See `docs/DEPLOYMENT.md` for the full rotation runbook and the deploy checks.

## Verification checklist

Before declaring a change done:

- [ ] `./gradlew ktlintCheck` passes.
- [ ] `./gradlew build` and `./gradlew test` pass (or explain why they could not run).
- [ ] `grep -RIn "api.telegram.org/bot"` / token patterns finds nothing new.
- [ ] New command/move appears in the keyboard, help text, rules, and docs.
- [ ] No secrets, build output, or IDE files added to git.
- [ ] `git diff` contains only intended changes.

## Out of scope unless asked

- Force-pushing, rewriting history, deleting branches, or publishing releases.
- Reworking the deploy workflow in `.github/workflows/deploy-render.yml`, or adding
  databases, Docker Compose, or a web dashboard.
- Refactoring the whole bot in one change.

## Related documents

- `README.md` — human-facing overview and run instructions.
- `CONTRIBUTING.md` — branch, test, and pull-request workflow.
- `SECURITY.md` — private vulnerability reporting and token-rotation steps.
- `CHANGELOG.md` — notable changes in Keep a Changelog format.
- `docs/ARCHITECTURE.md` — deeper design and flow.
- `docs/DEPLOYMENT.md` — Render setup, GitHub secrets, and token rotation.
- `docs/KNOWN_ISSUES.md` — current status of known findings.
- `.github/copilot-instructions.md` — short pointer for GitHub Copilot.
