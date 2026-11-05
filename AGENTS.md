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
  versions in `gradle/libs.versions.toml`.
- **Size:** 11 Kotlin source files, 4 test files, GitHub Actions CI, no persistence.
- **Deployment:** single Docker image on **Render** (Docker web service, health check
  `/healthz`). The bot uses Telegram **long polling**, so no webhook is required.
  `.github/workflows/deploy-render.yml` pushes `secrets.BOT_TOKEN` to Render and
  triggers a deploy. The service stays on the free plan,
  and `.github/workflows/keepalive.yml` pings `/healthz` every 5 minutes so Render
  does not sleep and stop polling.

Repository root is the directory containing this file.

## Secrets and configuration

The bot reads its token from the **`BOT_TOKEN` environment variable** and fails fast
when it is missing. `PORT` optionally overrides the HTTP port (default `8080`).

```bash
export BOT_TOKEN="<your-token>"
export PORT=8080            # optional
```

Rules for agents:

1. **Never** print, copy, log, echo, or commit real secrets. Use `BOT_TOKEN` /
   `<your-token>` placeholders in docs, tests, fixtures, and commit messages.
2. `.env` is git-ignored; `.env.example` documents the variable names.
3. tgbotapi includes the Telegram request URL (and therefore the token) in error
   messages. `src/main/resources/logback.xml` redacts `bot<id>:<secret>` from every
   message and stack trace; do not remove that pattern, and add a test with a fake
   token if you change it.
4. The old token was purged from git history with `git filter-repo`, but a token
   that was ever committed must still be treated as compromised and rotated via
   [@BotFather](https://t.me/BotFather). Do **not** rewrite history again unless the
   owner explicitly asks.
5. Do not add tokens to `Dockerfile`, `build.gradle.kts`, source files, or workflow
   files.
6. Production tokens live in GitHub Actions secrets: `BOT_TOKEN`,
   `RENDER_API_KEY`, plus the `RENDER_SERVICE_ID` repository variable. The deploy
   workflow copies `BOT_TOKEN` to Render at deploy time.

## Quick start

Prerequisites: JDK 21 and network access for Gradle dependencies. The current
sources were verified with Temurin JDK 21 (`./gradlew clean test buildFatJar`).

```bash
export BOT_TOKEN="<your-token>"

./gradlew build          # compile + assemble + run tests
./gradlew test           # unit tests only
./gradlew run            # starts the bot (long polling) and Ktor on :8080
./gradlew buildFatJar    # build/libs/rock-paper-scissors-lizard-spock-bot-all.jar
java -jar build/libs/rock-paper-scissors-lizard-spock-bot-all.jar

curl http://localhost:8080/healthz   # -> OK
```

Docker:

```bash
docker build -t rpsls-bot .
docker run --rm -p 8080:8080 -e BOT_TOKEN="<your-token>" rpsls-bot
```

Notes:

- Ktor binds `0.0.0.0:$PORT` (default 8080) and only serves `/healthz`. All real
  functionality goes through Telegram long polling.
- The bot does nothing useful without a valid token and outbound HTTPS access to
  `api.telegram.org`.

## Architecture at a glance

```text
Application.kt                        main(): reads config, starts Ktor
  │                                   (GET /healthz), then runBlocking { runTelegramBot }
  ├─ config/AppConfig.kt              BOT_TOKEN / PORT from the environment
  ├─ infrastructure/http/             GET /healthz route
  └─ infrastructure/telegram/         creates bot, registers handlers,
                                      buildBehaviourWithLongPolling { ... }.join()
       ├─ domain/*.kt                 move enums
       └─ application/*.kt            stateless game rules, return a String
```

| Path | Responsibility |
| --- | --- |
| `src/main/kotlin/com/el_giancar/Application.kt` | Composition root: reads config, starts Ktor with `/healthz`, wires services, then runs Telegram on the main thread. |
| `src/main/kotlin/com/el_giancar/config/AppConfig.kt` | Reads `BOT_TOKEN` and `PORT`; fails fast when the token is missing. |
| `src/main/kotlin/com/el_giancar/domain/ClassicMoveType.kt` | `ROCK/PAPER/SCISSORS` enum. |
| `src/main/kotlin/com/el_giancar/domain/SheldonMoveType.kt` | `ROCK/PAPER/SCISSORS/LIZARD/SPOCK` enum. |
| `src/main/kotlin/com/el_giancar/application/ClassicGameService.kt` | Random move + classic win/lose/tie logic. |
| `src/main/kotlin/com/el_giancar/application/SheldonGameService.kt` | Random move + 5-move win/lose/tie logic. |
| `src/main/kotlin/com/el_giancar/application/ChatLocks.kt` | One coroutine `Mutex` per numeric chat id. |
| `src/main/kotlin/com/el_giancar/infrastructure/http/HealthRoutes.kt` | `GET /healthz` route. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/TelegramBot.kt` | Bot wiring: commands, game flow, input parsing, per-chat locking. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Keyboards.kt` | Reply keyboards. |
| `src/main/kotlin/com/el_giancar/infrastructure/telegram/Messages.kt` | User-facing text. |
| `src/test/kotlin/com/el_giancar/application/*Test.kt` | Exhaustive pair tables for both games. |
| `src/test/kotlin/com/el_giancar/config/AppConfigTest.kt` | Environment parsing and fail-fast behavior. |
| `src/test/kotlin/com/el_giancar/infrastructure/http/HealthRouteTest.kt` | Ktor test for `GET /healthz`. |
| `src/test/kotlin/com/el_giancar/infrastructure/logging/LogbackConfigTest.kt` | Verifies that bot tokens are redacted from logs. |
| `src/main/resources/logback.xml` | Console logging at `info`, with bot-token redaction. |
| `src/main/resources/openapi/documentation.yaml` | Documents the `/healthz` endpoint (not served by Ktor). |
| `Dockerfile`, `.dockerignore` | Two-stage JDK 21 build, fat jar runtime. |
| `.github/workflows/ci.yml` | CI: `./gradlew build buildFatJar` on Temurin 21. |
| `.github/workflows/deploy-render.yml` | Updates `BOT_TOKEN` on Render and triggers a deploy. |
| `.github/workflows/keepalive.yml` | Pings `/healthz` every 5 minutes so the free Render service stays awake. |
| `docs/DEPLOYMENT.md` | Render setup, secrets, and token-rotation runbook. |
| `build.gradle.kts`, `gradle/libs.versions.toml` | Build logic and dependency versions. |

### Telegram update flow

1. `main()` builds `AppConfig` from `BOT_TOKEN` (failing fast if absent) and `PORT`,
   starts Ktor and its `/healthz` route, then calls `runTelegramBot(...)`, which
   builds the bot and both services and starts `buildBehaviourWithLongPolling`.
2. `onCommand("classic_game" | "sheldon_game")` acquires a **per-chat mutex** so two
   games cannot run in the same chat at once. If the chat is busy, the user is told
   to finish the current game.
3. The handler sends a prompt with a one-time reply keyboard and waits for a text
   message **from the same chat** with `waitTextMessage().first { ... }`.
4. Input is trimmed and matched case-insensitively against the enum names. Invalid
   input gets a hint and up to `MAX_MOVE_ATTEMPTS` (3) retries; any command cancels
   the game.
5. The corresponding typed service overload generates/accepts a computer move and
   returns a result string.
6. The bot replies with the result and re-sends the commands keyboard.
7. `/start`, `/rules`, `/help`, `/about`, `/commands` reply directly; unknown
   commands get a hint via `onUnhandledCommand`.

There is **no state**: no database, no scores, no sessions, no per-chat storage.
Each command is a short-lived coroutine; the only shared state is the in-memory
mutex map used to serialize games per chat.

## Game rules (source of truth: the services)

| Move | Beats |
| --- | --- |
| ROCK | SCISSORS, LIZARD |
| PAPER | ROCK, SPOCK |
| SCISSORS | PAPER, LIZARD |
| LIZARD | PAPER, SPOCK |
| SPOCK | ROCK, SCISSORS |

The classic variant uses only ROCK/PAPER/SCISSORS. Ties are equal moves. The
services hold a `winningPhrases` map keyed by `(winner, loser)` and return prose such
as `"You win! Spock vaporizes Rock."`, `"It's a tie!"`, `"You lose! ..."`.

When adding a winning relation, add it to **both directions of lookup** by updating
the map once — losses are derived from the same map.

## Conventions

- Package layout is `com.el_giancar.{config,domain,application,infrastructure}`; put
  environment/config in `config`, pure types in `domain`, game rules in `application`,
  and Ktor/Telegram adapters in `infrastructure`.
- Keep services **stateless** and free of tgbotapi imports so they stay unit-testable.
- Services accept **enum values**, not raw strings. Parsing/validation belongs in the
  Telegram layer.
- Each service exposes two overloads: `play(move)` (random opponent) and
  `play(playerMove, computerMove)` (deterministic, used by tests).
- Move enums use `UPPER_SNAKE_CASE`; button labels are enum `name`s.
- Kotlin official style, 4-space indentation, LF line endings (see `.editorconfig`).
- Keep dependency versions in `gradle/libs.versions.toml`; avoid hardcoding versions in
  `build.gradle.kts`.
- Prefer additive, focused changes. Do not reformat or rename unrelated code in a
  bug-fix change.
- Add new dependencies only when necessary; this project is intentionally small.

## Known pitfalls

- There is **no per-game timeout**. A pending game occupies its chat's mutex until
  the user answers or sends a command. If you add a timeout, release the mutex in a
  `finally` block.
- `waitTextMessage()` expects the update flow of the command's subcontext. Command
  handlers already filter by chat (`MessageFilterByChat`); the explicit chat-id check
  is a second safety net. Do not remove either without evaluating concurrency.
- `ChatIdentifier`/`ChatId` is a value class; the mutex map is keyed by the underlying
  `Long` (`message.chat.id.chatId.long`).
- The OpenAPI YAML is documentation only; no Ktor route serves it.
- tgbotapi 37 sets the HTTP request timeout equal to Telegram's long-poll timeout,
  so an idle bot logs an `HttpRequestTimeoutException` roughly every 30 seconds. The
  polling loop recovers on the next request and updates are not lost. Do not remove
  the log redaction pattern because that exception message contains the token.
- Render's free plan sleeps after 15 minutes without inbound traffic. The scheduled
  `keepalive.yml` workflow pings `/healthz`; keep it enabled while on the free plan.
- `getNames<T>()` was removed together with string-based parsing. Do not reintroduce
  it unless it has a real caller.
- The old bot token was removed from all commits, but copies/forks/caches may still
  contain it; rotation is an owner action.

## Common tasks

### Add a Telegram command

1. Register it in `src/main/kotlin/com/el_giancar/infrastructure/telegram/TelegramBot.kt`
   with `onCommand("name") { message -> ... }`.
2. Add a `simpleButton("/name")` to `commandsKeyboard()` in `Keyboards.kt` if it should
   be discoverable.
3. Add it to `helpMessage` in `Messages.kt` and to `README.md`.
4. Keep the reply style consistent with the existing messages (Unicode escapes such as
   `\uD83D\uDD25` are used for emoji).

### Add or change a move / rule

1. Add the enum constant to the relevant `domain/*MoveType.kt`.
2. Add/update the `winningPhrases` entries in the matching application service. A move
   must beat exactly two others in the five-move game.
3. Update the `/rules` text and `helpMessage` in `infrastructure/telegram/Messages.kt`.
4. Update the rules table in this file, `README.md`, and `docs/ARCHITECTURE.md`.
5. Extend the exhaustive test table in `src/test/kotlin/...` so all pairs stay
   covered.

### Add tests

Tests live in `src/test/kotlin`. Use `kotlin.test` on JUnit 5 (`kotlin-test-junit5` is
already declared). Prefer the deterministic `play(player, computer)` overload; no
random stubbing is required. Run with `./gradlew test`.

### Change configuration

Read environment variables in `AppConfig.fromEnvironment()`, fail fast with a clear
message, and update `.env.example`, `README.md`, and `AGENTS.md`. Never add a default
token.

### Rotate the bot token

1. Revoke the old token and create a new one with @BotFather (owner action).
2. Store it as the GitHub secret: `gh secret set BOT_TOKEN`.
3. Run the deploy workflow: `gh workflow run deploy-render.yml`. It updates the
   Render environment variable and redeploys.
4. Confirm the deploy is live (`render deploys list <srv-id>`) and `/healthz` returns
   `OK`, then send `/start` to the bot.
5. Confirm Render logs show `bot<redacted>` and not the token.

See `docs/DEPLOYMENT.md`. History was already purged; do not rewrite it again without
explicit owner approval and coordination with all clones.

## Verification checklist

Before declaring a change done:

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
- `docs/ARCHITECTURE.md` — deeper design and flow.
- `docs/DEPLOYMENT.md` — Render setup, GitHub secrets, and token rotation.
- `docs/KNOWN_ISSUES.md` — current status of known findings.
- `.github/copilot-instructions.md` — short pointer for GitHub Copilot.
