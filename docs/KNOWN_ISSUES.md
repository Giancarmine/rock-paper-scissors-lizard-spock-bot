# Known issues and findings

Status as of 2026-11-04 against `main` plus the remediation change. This document is
both a changelog of the review and a list of what still needs owner action.

## Resolved in the remediation change

| # | Area | Finding | Resolution |
| --- | --- | --- | --- |
| 1 | Security | Bot token hardcoded in `plugins/Telegram.kt` | Token is read from `BOT_TOKEN`; the app fails fast if unset. The source no longer contains a token. |
| 2 | Robustness | Invalid/lowercase input caused `NullPointerException` via `playerMove!!` | Input is trimmed, matched case-insensitively, retried up to 3 times, then cancelled cleanly. No `!!`. |
| 3 | Correctness | `/classic_game` validated against `SheldonMoveType`, accepting LIZARD/SPOCK | Services now take enum values; the classic handler parses against `ClassicMoveType` only. |
| 4 | UX | `/help` and `/about` buttons were not implemented | Both commands are registered with real content and listed in help. |
| 5 | Concurrency | Games could race or consume each other's replies | Per-chat `Mutex` serializes games; `waitTextMessage` is filtered to the originating chat. |
| 6 | Packaging | `EXPOSE 8080:8080`, JDK 17 build vs JDK 11 runtime, unused `ARG TOKEN`, hardcoded jar name, `gradle build` vs fat jar | Multi-stage JDK 17 build with `buildFatJar`, Temurin 17 runtime, `EXPOSE 8080`, runtime `BOT_TOKEN`, stable `/app/app.jar`. |
| 7 | Dependencies | Unused/out-of-sync `ktor-client-okhttp:2.0.1` | Removed. |
| 8 | Correctness | Unreachable `else` branches re-rolled the RNG | Replaced by a single `error(...)` path in rule lookup. |
| 9 | API surface | OpenAPI spec described non-existent routes; no health endpoint | Added `GET /healthz`; the spec now documents only that endpoint. |
| 10 | Process | No tests or CI; noisy `trace` logging | Added exhaustive service tests, a health-route test, a GitHub Actions workflow, and `info` logging. |

## Still requires owner action

### Rotate the leaked token (high priority)

The token that used to be hardcoded is still recoverable from git history (commit
`d1be367`). Removing it from the working tree does not revoke it.

1. Revoke the token in [@BotFather](https://t.me/BotFather) and issue a new one.
2. Set the new token as `BOT_TOKEN` in the runtime environment / platform secret
   store.
3. Optionally purge history with `git filter-repo` and force-push — only with
   explicit approval and coordination with everyone who has a clone.

Until rotation happens, assume the old token is usable by anyone with repository
access.

## Remaining limitations (accepted for now)

- **No game timeout.** A pending game holds the chat's mutex until the user replies
  or sends a command. A timeout can be added with `withTimeoutOrNull` around
  `awaitMove`; release the lock in `finally`.
- **No persistence or scores.** Every round is independent.
- **No license.** Treat as all rights reserved unless the owner adds one.
- **OpenAPI file is not served.** It is documentation only; if consumers need it
  over HTTP, add a route.
- **No Telegram API retry/backoff.** Network errors propagate to the handler
  coroutine.
- **The per-chat mutex map is never evicted.** Entries are small, but the map grows
  with the number of distinct chats seen since startup.
- **`/cancel` is not a registered command.** During a game any command cancels, but
  outside a game `/cancel` falls through to the generic "use /commands" reply. Add a
  handler if that is confusing.
- **The health endpoint only proves the HTTP server is up.** It does not verify that
  long polling is connected to Telegram.

## Verification performed

- Source review of all Kotlin files, Gradle files, Dockerfile, and resources.
- `grep` for the old token: present only in `.git` history, not in tracked files.
- Unit tests added for all 9 classic pairs and all 25 Sheldon pairs, plus canonical
  phrase checks, and a `testApplication` check for `GET /healthz`.
- `./gradlew clean test buildFatJar` passes with Temurin JDK 17; the fat jar starts
  and fails fast with a clear message when `BOT_TOKEN` is missing.
- Runtime smoke test of the Telegram path requires a valid token and was not
  performed.
