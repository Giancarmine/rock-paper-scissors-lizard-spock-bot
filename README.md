# Rock–Paper–Scissors–Lizard–Spock Bot

[![CI](https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot/actions/workflows/ci.yml/badge.svg)](https://github.com/Giancarmine/rock-paper-scissors-lizard-spock-bot/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![JVM](https://img.shields.io/badge/JVM-21-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)

A small Telegram bot that plays **Rock–Paper–Scissors** and the extended
**Rock–Paper–Scissors–Lizard–Spock** variant against you. It picks a random move
and answers with a human-readable result.

Written in Kotlin with [Ktor](https://ktor.io/) and
[tgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI), packaged as a
Docker image.

## Features

- Two game modes: classic (3 moves) and Sheldon (5 moves).
- Reply keyboards with readable move labels (`Rock`, `Paper`, `Lizard`, ...).
- Every result shows your move, the bot's move, and the winning relation.
- `/rules` prints the full rules with a consistent, friendly tone.
- Case-insensitive input with up to three attempts before the game is cancelled.
- One game per chat at a time, with a two-minute timeout so a chat cannot stay locked.
- `/cancel` stops the current game, and explains when there is nothing to cancel.
- Plain text in an idle private chat gets a short hint instead of silence; group chats are left alone.
- `/healthz` and `/readyz` HTTP endpoints for hosting platforms and monitors.
- `/openapi.yaml` serves the HTTP API description from the running process.
- No accounts, scores, or database — every round is independent.
- Uses Telegram long polling, so no public URL or webhook is required.

## Commands

| Command | Description |
| --- | --- |
| `/start` | Greeting and command keyboard. |
| `/rules` | Rules of Rock–Paper–Scissors–Lizard–Spock. |
| `/classic_game` | Play with Rock, Paper, Scissors. |
| `/sheldon_game` | Play with Rock, Paper, Scissors, Lizard, Spock. |
| `/cancel` | Cancel the current game (or report that none is running). |
| `/help` | List the available commands. |
| `/about` | About the bot and its source. |
| `/commands` | Show the command keyboard. |
| anything else | Replies with a hint to use `/commands`. |

During a game, the move keyboard includes `/cancel`; any other command also cancels.
Outside a game, plain text in a private chat gets a hint to start a game, while group
chats are not interrupted.

## Rules

```text
Scissors cuts Paper
Paper covers Rock
Rock crushes Lizard
Lizard poisons Spock
Spock smashes Scissors
Scissors decapitates Lizard
Lizard eats Paper
Paper disproves Spock
Spock vaporizes Rock
(and as it always has) Rock crushes Scissors
```

| Move | Beats |
| --- | --- |
| Rock | Scissors, Lizard |
| Paper | Rock, Spock |
| Scissors | Paper, Lizard |
| Lizard | Paper, Spock |
| Spock | Rock, Scissors |

## Tech stack

- Kotlin/JVM 2.4.21
- Ktor 3.6.1 (Netty)
- tgbotapi 37.1.0 (`dev.inmo`)
- Gradle 9.8.2 (wrapper committed)
- Logback 1.6.6
- JUnit 5 (Jupiter)
- ktlint via the Gradle plugin 14.2.0
- Docker (multi-stage, JDK 21)

## Getting started

### Prerequisites

- JDK 21 (used for both the Docker build and runtime)
- A Telegram bot token from [@BotFather](https://t.me/BotFather)

### Configure

The token is read from the `BOT_TOKEN` environment variable. The application fails
fast with a clear message if it is missing. `PORT` is optional (default `8080`).

```bash
cp .env.example .env      # then edit .env
export BOT_TOKEN="<your-token>"
export PORT=8080          # optional
```

> `BOT_TOKEN` is required at runtime. `.env` is git-ignored; never commit a real
> token. Production tokens live only in GitHub Actions secrets and the hosting
> platform, never in the repository.

### Run locally

```bash
./gradlew run
```

The process starts Ktor on `0.0.0.0:8080` (only `/healthz` and `/readyz` are
exposed) and then begins Telegram long polling. Send `/start` to your bot.

### Build a jar

```bash
./gradlew buildFatJar
java -jar build/libs/rock-paper-scissors-lizard-spock-bot-all.jar
```

### Run with Docker

```bash
docker build -t rpsls-bot .
docker run --rm -p 8080:8080 -e BOT_TOKEN="<your-token>" rpsls-bot
```

The image is multi-stage: Gradle 9.8.2 + JDK 21 to build the fat jar, then a
Temurin 21 JRE runtime. The token is injected at runtime and is not baked into the
image.

### Tests

```bash
./gradlew test
```

`src/test/kotlin` contains exhaustive pair tables for both game modes, checks for the
canonical winning phrases, formatting tests for the user-facing messages, move parser
tests, `GameFlow` tests for valid moves, retries, cancellation, timeout and a busy
chat, idle-text hint tests, HTTP route tests for `/healthz`, `/readyz` and
`/openapi.yaml`, and log-redaction/timeout-filter tests. Tests run on JUnit 5 via
`kotlin-test-junit5`.

### Lint

```bash
./gradlew ktlintCheck   # fail on style violations
./gradlew ktlintFormat  # apply the safe fixes
```

`.editorconfig` disables only `standard:package-name`, because `com.el_giancar` is
intentional.

### Health checks

```bash
curl http://localhost:8080/healthz     # OK (liveness)
curl http://localhost:8080/readyz      # READY once Telegram is connected, else 503
curl http://localhost:8080/openapi.yaml  # OpenAPI 3.0 document
```

`/healthz` only proves the process is up, so a Telegram outage does not cause a
restart loop. `/readyz` reports whether the bot is connected; the state is set by the
startup `getMe()` and refreshed every 60 seconds. `/openapi.yaml` is read from the
bundled `openapi/documentation.yaml` once at startup.

## Project layout

```text
src/main/kotlin/com/el_giancar/
├── Application.kt                     # composition root: config, Ktor, bot
├── config/
│   └── AppConfig.kt                   # BOT_TOKEN / PORT from the environment
├── domain/
│   ├── Move.kt                        # move contract + display name
│   ├── GameOutcome.kt                 # WIN / LOSE / TIE
│   ├── RoundResult.kt                 # typed result of one round
│   ├── ClassicMoveType.kt             # 3-move enum
│   └── SheldonMoveType.kt             # 5-move enum
├── application/
│   ├── Game.kt                        # playable game contract
│   ├── GameRules.kt                   # legal moves + winning phrases
│   ├── GameService.kt                 # shared round resolution
│   ├── ClassicGameService.kt          # classic rules
│   ├── SheldonGameService.kt          # 5-move rules
│   ├── ChatLocks.kt                   # per-chat mutex, evicted when idle
│   └── ReadinessState.kt              # Telegram connectivity for /readyz
└── infrastructure/
    ├── http/
    │   ├── HealthRoutes.kt            # GET /healthz + /readyz
    │   └── OpenApiRoutes.kt           # GET /openapi.yaml
    ├── logging/
    │   ├── JulBridge.kt               # JUL -> SLF4J so token redaction applies
    │   └── ExpectedLongPollTimeoutFilter.kt # drops idle getUpdates timeouts
    └── telegram/
        ├── TelegramBot.kt             # command wiring + readiness monitor
        ├── GameFlow.kt                # prompt, timeout, lock, result
        ├── GameIo.kt                  # send/await seam for flow tests
        ├── MoveParser.kt              # text -> move
        ├── Replies.kt                 # shared reply helpers
        ├── Keyboards.kt               # reply keyboards
        └── Messages.kt                # user-facing text
src/main/resources/
├── logback.xml
└── openapi/documentation.yaml         # documents /healthz and /readyz
src/test/kotlin/com/el_giancar/
├── application/                       # game-rule tests
├── config/                            # environment config tests
├── infrastructure/http/               # /healthz, /readyz and /openapi.yaml route tests
├── infrastructure/logging/            # log redaction, JUL bridge, timeout filter tests
└── infrastructure/telegram/           # messages, move parser, GameFlow, idle-text tests
```

## Known limitations

These are deliberate trade-offs rather than open bugs. See
[`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) for the full history and the
verification that backs each item.

**State and scaling**

- **No persistence.** Games, scores, and chat locks live in memory, so every round is
  independent and a restart forgets in-flight games.
- **One instance per token.** Telegram long polling allows a single consumer; a second
  replica would get `409 Conflict`. Horizontal scaling needs webhooks and a shared
  store.
- **No retry/backoff for replies.** Telegram or network errors surface to the handler
  coroutine. The long-poll loop reconnects on the next cycle, but a failed reply is
  not resent.

**Hosting**

- **Render free-plan sleep.** The service stops after 15 minutes without inbound
  traffic. The scheduled keep-alive workflow mitigates this, but GitHub can delay
  scheduled runs, so the first message after a rare sleep may wait for a cold start.
  Telegram keeps updates for 24 hours, so nothing is lost.
- **Readiness lags by up to 60 seconds.** `/readyz` reflects the last periodic
  `getMe()`, not a live call per request.

**Behaviour and logging**

- **Idle long-poll timeout noise.** tgbotapi's HTTP timeout equals Telegram's
  `getUpdates` timeout, so an idle bot raises `HttpRequestTimeoutException` about
  every 30 seconds. A Logback filter drops exactly those expected events; other
  timeouts and errors still appear, and tokens are always redacted.
- **Pseudo-random moves.** `java.util.Random` is fine for a casual game but not
  suitable when stakes or fairness guarantees matter.
- **English only.** Messages and commands are not localised.
- **Telegram only.** There is no web or mobile front end; HTTP is limited to the
  health probes and the OpenAPI document.
- **A game that receives no move for two minutes is cancelled automatically**, and
  the reply keyboard and chat lock are released.

See [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) for details.

## Deployment

The bot runs as a Docker web service on [Render](https://render.com/). Pushing a
`v*` tag or running the **Deploy to Render** workflow:

1. Updates the service's `BOT_TOKEN` environment variable from the
   `BOT_TOKEN` GitHub secret.
2. Triggers a Render deploy.

`BOT_TOKEN` is never committed or baked into the image. One-time setup, the required
GitHub secrets/variables, and the token-rotation runbook are documented in
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

### Free plan

The service stays on Render's **free** plan. Free web services stop after 15 minutes
without inbound traffic, which would also stop Telegram long polling, so
[`.github/workflows/keepalive.yml`](.github/workflows/keepalive.yml) pings
`/readyz` every 5 minutes. One always-on free service uses under the monthly
instance-hour allowance. GitHub can delay scheduled runs, so a message sent right
after a rare sleep may wait until the next successful ping; Telegram keeps updates
for 24 hours, so nothing is lost.

Update `READINESS_URL` in that workflow if the Render URL changes (for example in
a fork).

## Documentation for contributors and AI agents

- [`CONTRIBUTING.md`](CONTRIBUTING.md) — branch, test, and pull-request workflow.
- [`SECURITY.md`](SECURITY.md) — private vulnerability reporting and rotation steps.
- [`CHANGELOG.md`](CHANGELOG.md) — notable changes and release notes.
- [`AGENTS.md`](AGENTS.md) — instructions for AI coding agents.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — components and update flow.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — Render setup and token rotation.
- [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — status of reviewed findings.

## License

Released under the [MIT License](LICENSE).
