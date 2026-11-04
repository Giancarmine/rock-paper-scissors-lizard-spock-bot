# GitHub Copilot instructions

This repository's canonical agent guidance lives in [`AGENTS.md`](../AGENTS.md).
Read it first; this file is only a short pointer.

Key points:

- Kotlin/JVM + Ktor + tgbotapi Telegram bot. Entry point:
  `src/main/kotlin/com/el_giancar/Application.kt`; bot wiring:
  `src/main/kotlin/com/el_giancar/plugins/Telegram.kt`; game rules:
  `src/main/kotlin/com/el_giancar/srv/`.
- The token is read from the `BOT_TOKEN` environment variable. **Never** print, copy,
  or commit a real token. A previously committed token remains in git history and
  must be rotated.
- Game services are stateless and Telegram-free; they accept enum values, not
  strings. Each exposes `play(move)` and deterministic `play(player, computer)`.
- Input parsing, case-insensitive matching, retries, and per-chat locking live in
  `Telegram.kt`.
- Add tests under `src/test/kotlin` and run `./gradlew test`.
- Prefer focused changes; do not reformat unrelated code.

Details: [`AGENTS.md`](../AGENTS.md), [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md),
[`docs/KNOWN_ISSUES.md`](../docs/KNOWN_ISSUES.md).
