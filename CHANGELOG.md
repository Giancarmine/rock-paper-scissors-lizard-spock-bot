# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html). The version
in `build.gradle.kts` is the source of truth for releases.

## [Unreleased]

### Added

- A dispatch-only `configure-bot-metadata.yml` workflow that applies the bot display name,
  short description, and description through the Bot API. The `/` command menu is
  registered by the bot at startup instead.
- Growth loops: the command menu is synchronized at startup, a completed round offers a
  Telegram share button, adding the bot to a group posts a short welcome, and `/start`
  deep links are attributed and answered with a share-specific greeting.
- Group milestone announcements for a new chat leader or a round point total.
- Inline mode: `@RockPaperScissorsLizardSpockBot` returns Play and Share cards for the
  classic and five-move games (requires `/setinline` in BotFather).
- Optional PostgreSQL score tracking with `/stats`, `/leaderboard`, and
  `/chat_leaderboard`. Wins are worth one point; ties and losses are worth zero.
- HikariCP + PostgreSQL JDBC in production, H2 in PostgreSQL mode for repository tests,
  and automatic schema creation on first startup.
- `DATABASE_URL` (plus optional `DATABASE_USER` / `DATABASE_PASSWORD`) configuration and
  optional Render environment updates in the deploy workflow.
- Graceful stats fallback: without a database the bot still plays, and a failed read or
  write only makes the stats commands report that tracking is disabled or unavailable.
- MIT license.
- `GET /openapi.yaml`, which serves the OpenAPI 3.0 document bundled with the
  application.
- `ExpectedLongPollTimeoutFilter`, which drops the expected `getUpdates` timeout on
  an idle bot while keeping real errors.
- Dependabot configuration for Gradle, GitHub Actions, and Docker base images.
- Contributor and security guides, issue and pull-request templates, and a changelog.
- ktlint formatting checks in CI (Gradle plugin 14.2.0).

### Changed

- Raised the stats pool connection timeout to 10 seconds and added PostgreSQL
  connect/login/socket timeouts so a Neon cold start or a stalled socket cannot hang a
  stats request indefinitely.
- `GameFlow.play` now returns the completed `RoundResult` (or `null` for cancellations,
  timeouts, busy chats, and invalid attempts) so only finished rounds are recorded.
- Expanded the README badges, project layout, and known-limitations section.
- Documented the OpenAPI route in `docs/ARCHITECTURE.md` and `AGENTS.md`.
- Corrected the documented HTTP surface, JUnit version, and timeout-filter guidance in
  the README and deployment runbook.
- Refined the bot's user-facing texts: clearer prompts and errors, consistent command
  descriptions, retry counts on invalid moves, and a license note in `/about`.
- Applied ktlint formatting across the Kotlin sources.

### Fixed

- Group chats: only the player who started a game can supply its move. Other members'
  text no longer plays (or cancels) someone else's game, and the round is credited to
  the right player.
- Score tracking retries schema initialization on the first game or stats request
  instead of staying disabled until a restart when the database was asleep or
  unreachable at startup.
- Completed rounds dismiss the move-selection reply keyboard before showing the share
  button, so the move buttons no longer linger after a result.

### Security

- Removed stale guidance that the previously exposed token still needed rotation;
  the token has been rotated and no secret is committed.

## [0.0.1] - 2026-11-06

### Added

- Rock–Paper–Scissors and Rock–Paper–Scissors–Lizard–Spock game modes with reply
  keyboards, typed round results, and human-readable rules.
- `/start`, `/rules`, `/help`, `/about`, `/commands`, `/classic_game`,
  `/sheldon_game`, and `/cancel` commands.
- Per-chat game locks with eviction, a two-minute game timeout, and idle-text hints.
- `GET /healthz` liveness and `GET /readyz` Telegram-readiness probes.
- Token redaction in Logback output plus a JUL-to-SLF4J bridge for `tgbotapi` logs.
- Multi-stage Docker image, GitHub Actions CI, and a Render deploy workflow that
  keeps the service on the free plan.
- Unit and route tests for game rules, message rendering, parsing, locks, `GameFlow`,
  HTTP routes, and log redaction.
