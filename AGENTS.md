# AGENTS.md

Guidance for AI coding agents (OpenCode, GitHub Copilot, Cursor, Claude Code, etc.)
working in this repository. Read this before changing code.

## Project snapshot

- **What it is:** a Telegram chat bot that plays _Rock–Paper–Scissors_ and
  _Rock–Paper–Scissors–Lizard–Spock_ against the user. The bot picks a random move
  and replies with a human-readable result.
- **Language/runtime:** Kotlin/JVM 1.7.22 on Java 17 (used for both build and
  runtime images).
- **Frameworks:** Ktor 2.2.3 (Netty server), [`dev.inmo:tgbotapi`](https://github.com/InsanusMokrassar/TelegramBotAPI) 5.1.0
  for Telegram, Logback for logging.
- **Build:** Gradle 7.5.1 via the committed wrapper (`./gradlew`).
- **Size:** 6 Kotlin source files, 3 test files, GitHub Actions CI, no persistence.
- **Deployment:** single Docker image on **Render** (Docker web service, health check
  `/healthz`). The bot uses Telegram **long polling**, so no webhook is required.
  `.github/workflows/deploy-render.yml` pushes `secrets.BOT_TOKEN` to Render and
  triggers a deploy.

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
3. The old token was purged from git history with `git filter-repo`, but a token
   that was ever committed must still be treated as compromised and rotated via
   [@BotFather](https://t.me/BotFather). Do **not** rewrite history again unless the
   owner explicitly asks.
4. Do not add tokens to `Dockerfile`, `build.gradle.kts`, source files, or workflow
   files.
5. Production tokens live in GitHub Actions secrets: `BOT_TOKEN`,
   `RENDER_API_KEY`, plus the `RENDER_SERVICE_ID` repository variable. The deploy
   workflow copies `BOT_TOKEN` to Render at deploy time.

## Quick start

Prerequisites: JDK 17 and network access for Gradle dependencies. The current
sources were verified with Temurin JDK 17 (`./gradlew clean test buildFatJar`).

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
Application.kt                 main(): reads BOT_TOKEN/PORT, starts Ktor
  │                            (GET /healthz), then runBlocking { configureTelegram }
  └─ configureHttp()           installs the /healthz route
       └─ plugins/Telegram.kt  creates bot, registers handlers,
                               buildBehaviourWithLongPolling { ... }.join()
            ├─ model/*.kt      move enums
            └─ srv/*.kt        stateless game rules, return a String
```

| Path | Responsibility |
| --- | --- |
| `src/main/kotlin/com/el_giancar/Application.kt` | Process entry point; reads `BOT_TOKEN`/`PORT`, starts Ktor with `/healthz`, then runs Telegram on the main thread. |
| `src/main/kotlin/com/el_giancar/plugins/Telegram.kt` | All bot wiring: env token, commands, keyboards, input parsing, per-chat locking. |
| `src/main/kotlin/com/el_giancar/model/ClassicMoveType.kt` | `ROCK/PAPER/SCISSORS` enum. |
| `src/main/kotlin/com/el_giancar/model/SheldonMoveType.kt` | `ROCK/PAPER/SCISSORS/LIZARD/SPOCK` enum. |
| `src/main/kotlin/com/el_giancar/srv/ClassicGameService.kt` | Random move + classic win/lose/tie logic. |
| `src/main/kotlin/com/el_giancar/srv/SheldonGameService.kt` | Random move + 5-move win/lose/tie logic. |
| `src/test/kotlin/com/el_giancar/srv/*Test.kt` | Exhaustive pair tables for both games. |
| `src/test/kotlin/com/el_giancar/HealthRouteTest.kt` | Ktor test for `GET /healthz`. |
| `src/main/resources/logback.xml` | Console logging at `info`. |
| `src/main/resources/openapi/documentation.yaml` | Documents the `/healthz` endpoint (not served by Ktor). |
| `Dockerfile`, `.dockerignore` | Two-stage JDK 17 build, fat jar runtime. |
| `.github/workflows/ci.yml` | CI: `./gradlew build buildFatJar` on Temurin 17. |
| `.github/workflows/deploy-render.yml` | Updates `BOT_TOKEN` on Render and triggers a deploy. |
| `docs/DEPLOYMENT.md` | Render setup, secrets, and token-rotation runbook. |
| `build.gradle.kts`, `gradle.properties` | Dependencies and versions. |

### Telegram update flow

1. `main()` reads `BOT_TOKEN` (failing fast if absent) and `PORT`, starts Ktor and
   its `/healthz` route, then calls `configureTelegram(token)`, which builds the bot
   and both services and starts `buildBehaviourWithLongPolling`.
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

- Package layout is `com.el_giancar.{model,srv,plugins}`; put new domain types in
  `model`, pure logic in `srv`, Telegram wiring in `plugins`.
- Keep services **stateless** and free of tgbotapi imports so they stay unit-testable.
- Services accept **enum values**, not raw strings. Parsing/validation belongs in the
  Telegram layer.
- Each service exposes two overloads: `play(move)` (random opponent) and
  `play(playerMove, computerMove)` (deterministic, used by tests).
- Move enums use `UPPER_SNAKE_CASE`; button labels are enum `name`s.
- Kotlin official style, 4-space indentation, LF line endings (see `.editorconfig`).
- Keep versions in `gradle.properties`; avoid hardcoding dependency versions in
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
  `Long` (`message.chat.id.chatId`).
- The OpenAPI YAML is documentation only; no Ktor route serves it.
- `getNames<T>()` was removed together with string-based parsing. Do not reintroduce
  it unless it has a real caller.
- The old bot token was removed from all commits, but copies/forks/caches may still
  contain it; rotation is an owner action.

## Common tasks

### Add a Telegram command

1. Register it in `src/main/kotlin/com/el_giancar/plugins/Telegram.kt` with
   `onCommand("name") { message -> ... }`.
2. Add a `simpleButton("/name")` to `printCommandsMenu` if it should be discoverable.
3. Add it to `helpMessage` and to `README.md`.
4. Keep the reply style consistent with the existing messages (Unicode escapes such as
   `\uD83D\uDD25` are used for emoji).

### Add or change a move / rule

1. Add the enum constant to the relevant `*MoveType`.
2. Add/update the `winningPhrases` entries in the matching service. A move must beat
   exactly two others in the five-move game.
3. Update the `/rules` text and `helpMessage` if needed.
4. Update the rules table in this file, `README.md`, and `docs/ARCHITECTURE.md`.
5. Extend the exhaustive test table in `src/test/kotlin/...` so all pairs stay
   covered.

### Add tests

Tests live in `src/test/kotlin`. Use `kotlin.test` (`kotlin-test-junit` is already
declared). Prefer the deterministic `play(player, computer)` overload; no random
stubbing is required. Run with `./gradlew test`.

### Change configuration

Read environment variables at startup in `configureTelegram()` or `main()`, fail
fast with a clear message, and update `.env.example`, `README.md`, and `AGENTS.md`.
Never add a default token.

### Rotate the bot token

1. Revoke the old token and create a new one with @BotFather (owner action).
2. Store it as the GitHub secret: `gh secret set BOT_TOKEN`.
3. Run the deploy workflow: `gh workflow run deploy-render.yml`. It updates the
   Render environment variable and redeploys.
4. Verify `/healthz` and send `/start` to the bot.

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
