# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the
project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html). The version
in `build.gradle.kts` is the source of truth for releases.

## [Unreleased]

### Added

- MIT license.
- `GET /openapi.yaml`, which serves the OpenAPI 3.0 document bundled with the
  application.
- `ExpectedLongPollTimeoutFilter`, which drops the expected `getUpdates` timeout on
  an idle bot while keeping real errors.
- Dependabot configuration for Gradle, GitHub Actions, and Docker base images.
- Contributor and security guides, issue and pull-request templates, and a changelog.
- ktlint formatting checks in CI (Gradle plugin 14.2.0).

### Changed

- Expanded the README badges, project layout, and known-limitations section.
- Documented the OpenAPI route in `docs/ARCHITECTURE.md` and `AGENTS.md`.
- Corrected the documented HTTP surface, JUnit version, and timeout-filter guidance in
  the README and deployment runbook.
- Refined the bot's user-facing texts: clearer prompts and errors, consistent command
  descriptions, retry counts on invalid moves, and a license note in `/about`.
- Applied ktlint formatting across the Kotlin sources.

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
