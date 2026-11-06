# Rock–Paper–Scissors–Lizard–Spock Bot

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
> token. The previously committed token was purged from git history, but tokens are
> compromised once exposed — rotate if a copy still exists anywhere.

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
chat, and idle-text hint tests. Tests run on JUnit 5 via `kotlin-test-junit5`.

### Health checks

```bash
curl http://localhost:8080/healthz   # OK (liveness)
curl http://localhost:8080/readyz    # READY once Telegram is connected, else 503
```

`/healthz` only proves the process is up, so a Telegram outage does not cause a
restart loop. `/readyz` reports whether the bot is connected; the state is set by the
startup `getMe()` and refreshed every 60 seconds.

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
    │   └── HealthRoutes.kt            # GET /healthz + /readyz
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
├── infrastructure/http/               # /healthz and /readyz route tests
├── infrastructure/logging/            # log redaction + JUL bridge tests
└── infrastructure/telegram/           # messages, move parser, GameFlow, idle-text tests
```

## Known limitations

- No persistence, scores, or leaderboards.
- A game that receives no move for two minutes is cancelled automatically; the reply
  keyboard and the chat lock are released.
- The OpenAPI document is descriptive only and is not served by the application.
- On Render's free plan the service sleeps without traffic; the scheduled keep-alive
  workflow mitigates this, but scheduled runs can be delayed.
- tgbotapi logs a timeout every 30 seconds while the bot is idle because its client
  request timeout equals Telegram's long-poll timeout. Log output redacts the bot
  token (JUL is bridged to SLF4J so tgbotapi's logs use the same pattern); the polling
  loop recovers on the next request.
- The previously committed bot token was purged from git history, but it must still
  be treated as compromised and rotated with @BotFather.

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

- [`AGENTS.md`](AGENTS.md) — instructions for AI coding agents.
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — components and update flow.
- [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) — Render setup and token rotation.
- [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) — status of reviewed findings.

## License

No license file is included in this repository. Unless the owner states otherwise,
treat it as all rights reserved.
