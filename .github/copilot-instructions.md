# GitHub Copilot instructions

This repository's canonical agent guidance lives in [`AGENTS.md`](../AGENTS.md).
Read it first; this file is only a short pointer.

Key points:

- Kotlin/JVM + Ktor + tgbotapi Telegram bot. Entry point:
  `src/main/kotlin/com/el_giancar/Application.kt`; bot wiring:
  `src/main/kotlin/com/el_giancar/infrastructure/telegram/`; game rules:
  `src/main/kotlin/com/el_giancar/application/`; move enums:
  `src/main/kotlin/com/el_giancar/domain/`.
- The token is read from the `BOT_TOKEN` environment variable. **Never** print, copy,
  or commit a real token. Production tokens live only in GitHub Actions secrets and
  the hosting platform.
- Production secrets live in GitHub Actions (`BOT_TOKEN`, `RENDER_API_KEY`); the
  Render deploy workflow copies `BOT_TOKEN` to the service at deploy time. See
  [`docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md).
- Game services are stateless and Telegram-free; they accept enum values, not
  strings. Each exposes `play(move)` and deterministic `play(player, computer)`.
- Input parsing, case-insensitive matching, retries, and per-chat locking live in
  `infrastructure/telegram/` (`MoveParser.kt`, `GameFlow.kt`, `TelegramBot.kt`).
- HTTP routes live in `infrastructure/http/`: `/healthz`, `/readyz`, and the
  bundled `/openapi.yaml`.
- Add tests under `src/test/kotlin` and run `./gradlew test`.
- Prefer focused changes; do not reformat unrelated code.

Details: [`AGENTS.md`](../AGENTS.md), [`CONTRIBUTING.md`](../CONTRIBUTING.md),
[`SECURITY.md`](../SECURITY.md), [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
[`docs/DEPLOYMENT.md`](../docs/DEPLOYMENT.md),
[`docs/KNOWN_ISSUES.md`](../docs/KNOWN_ISSUES.md).
