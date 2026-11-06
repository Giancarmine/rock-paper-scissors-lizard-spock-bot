# Known issues and findings

Status as of 2026-11-06 against `main` plus the remediation changes. This document
is both a changelog of the review and a list of what still needs owner action.

## Resolved in the remediation change

| # | Area | Finding | Resolution |
| --- | --- | --- | --- |
| 1 | Security | Bot token hardcoded in the Telegram layer | Token is read from `BOT_TOKEN`; the app fails fast if unset. The source no longer contains a token. |
| 2 | Robustness | Invalid/lowercase input caused `NullPointerException` via `playerMove!!` | Input is trimmed, matched case-insensitively, retried up to 3 times, then cancelled cleanly. No `!!`. |
| 3 | Correctness | `/classic_game` validated against `SheldonMoveType`, accepting LIZARD/SPOCK | Services now take enum values; the classic handler parses against `ClassicMoveType` only. |
| 4 | UX | `/help` and `/about` buttons were not implemented | Both commands are registered with real content and listed in help. |
| 5 | Concurrency | Games could race or consume each other's replies | Per-chat `Mutex` serializes games; `waitTextMessage` is filtered to the originating chat. |
| 6 | Packaging | `EXPOSE 8080:8080`, build/runtime JDK mismatch, unused `ARG TOKEN`, hardcoded jar name, `gradle build` vs fat jar | Multi-stage JDK build with `buildFatJar`, matching runtime, `EXPOSE 8080`, runtime `BOT_TOKEN`, stable `/app/app.jar`. Current images use JDK 21. |
| 7 | Dependencies | Unused/out-of-sync `ktor-client-okhttp:2.0.1` | Removed. |
| 8 | Correctness | Unreachable `else` branches re-rolled the RNG | Replaced by a single `error(...)` path in rule lookup. |
| 9 | API surface | OpenAPI spec described non-existent routes; no health endpoint | Added `GET /healthz`; the spec now documents only that endpoint. |
| 10 | Process | No tests or CI; noisy `trace` logging | Added exhaustive service tests, a health-route test, a GitHub Actions workflow, and `info` logging. |
| 11 | Security | Token remained recoverable from git history | History rewritten with `git filter-repo` and force-pushed; the commit contents are otherwise unchanged. |
| 12 | Security | tgbotapi error messages contain the request URL, so runtime logs exposed the token | `logback.xml` redacts `bot<id>:<secret>` from messages and stack traces; a test guards the behavior. |
| 15 | Security | tgbotapi logs through kslog, whose JVM backend uses `java.util.logging` directly, bypassing the Logback pattern; the token still appeared in plaintext in Render logs | `installJulToSlf4jBridge()` installs `SLF4JBridgeHandler` at startup so JUL records go through Logback and the redaction pattern. Guarded by `JulBridgeTest`. |
| 13 | Availability | The free Render service sleeps after 15 minutes without traffic, stopping long polling | `keepalive.yml` pings `/readyz` every 5 minutes; documented in `DEPLOYMENT.md`. The job allows 10 minutes and the curl retries allow 8m45s, so a cold start is not cancelled mid-wake-up. |
| 14 | UX / Architecture | User-facing prose lived in the game services, move labels were enum names, `TelegramBot` mixed wiring/parsing/flow, and an abandoned game locked the chat forever | Services return typed `RoundResult`; `Messages.kt` renders all text; moves have `displayName`; `GameFlow` + `MoveParser` split the adapter; games time out after two minutes. |
| 16 | Observability | `/healthz` only proved the HTTP server was up, so a dead Telegram session or revoked token still looked healthy | Added `/readyz`: `200` when connected, `503` otherwise. Readiness is set by the startup `getMe()` and refreshed by a 60-second `getMe()` monitor. `/healthz` stays a liveness probe so a Telegram outage does not cause a restart loop. |
| 17 | UX | `/cancel` was not registered, so outside a game it fell through to the generic "use /commands" reply | Registered `/cancel`, added it to the move keyboard and help text. During a game the prompt loop sends the cancellation reply; when idle the handler explains that no game is running. |
| 18 | Memory | The per-chat mutex map was never evicted | `ChatLocks` now pins entries with a reference count and evicts them through `ConcurrentHashMap.compute` when the last caller releases an unlocked lock. Pinning prevents two callers from acquiring different mutexes for the same chat. |
| 19 | Testability | The prompt loop could only be exercised through a real Telegram context, so retries, cancellation, timeout and the busy case were untested | Extracted a `GameIo` send/await seam with a real `TelegramGameIo`; `GameFlowTest` covers valid moves, invalid retries, too many attempts, `/cancel`, timeout, busy chats and lock release. |
| 20 | UX | Plain text sent outside a game was ignored, so a new user could think the bot was broken | Added an `onText` hint for idle private chats. It skips commands, chats where a game holds the lock (the prompt loop owns that text), and group chats so ordinary conversation is not interrupted. |
| 21 | Packaging / discoverability | There was no license and the OpenAPI document was only readable from the source tree | Added the MIT `LICENSE` and `GET /openapi.yaml`, which serves the bundled spec with `application/yaml`. `OpenApiRouteTest` covers the status, content type, and body. |
| 22 | Observability | An idle bot logged an expected `HttpRequestTimeoutException` at ERROR every ~30 seconds, burying real failures | Added `ExpectedLongPollTimeoutFilter`, which drops only timeouts whose URL targets `getUpdates`. Other timeouts and errors still reach the appenders; `ExpectedLongPollTimeoutFilterTest` covers direct, wrapped, and unrelated exceptions. |

## Security status

The token that once appeared in source and runtime logs has been rotated. No secret
is committed: production values live in GitHub Actions secrets and the Render
environment. `logback.xml` redacts `bot<id>:<secret>` from messages and stack traces,
and `installJulToSlf4jBridge()` routes tgbotapi's `java.util.logging` records through
Logback so the redaction also applies to library logs. See [`DEPLOYMENT.md`](DEPLOYMENT.md)
for the rotation runbook.

## Remaining limitations (accepted for now)

- **No persistence or scores.** Every round is independent, and a restart forgets
  in-flight games.
- **One instance per token.** Telegram long polling allows a single consumer; a second
  replica would receive `409 Conflict`. Horizontal scaling needs webhooks plus a shared
  store.
- **No Telegram API retry/backoff.** Network errors propagate to the handler
  coroutine. The long-poll loop reconnects on the next cycle, but failed replies are
  not resent.
- **Pseudo-random moves.** `java.util.Random` is fine for a casual game but not for
  anything with stakes.
- **English only.** Messages and commands are not localised.
- **Idle long-poll timeouts are filtered.** tgbotapi 37 sets the HTTP request timeout
  equal to Telegram's long-poll timeout, so an idle bot raises
  `HttpRequestTimeoutException` roughly every 30 seconds. The polling loop retries and
  no update is lost; `ExpectedLongPollTimeoutFilter` drops only the expected
  `getUpdates` events, and the jul-to-slf4j bridge keeps the token redacted.
- **Render free-plan sleep.** The service stops after 15 minutes without inbound
  traffic. `.github/workflows/keepalive.yml` pings `/readyz` every 5 minutes, but
  GitHub can delay scheduled runs. Updates remain queued in Telegram for 24 hours.
- **Readiness lags a real outage by up to 60 seconds.** The periodic `getMe()` check
  runs every 60 seconds; `/readyz` does not call Telegram on each request.

## Verification performed

- Source review of all Kotlin files, Gradle files, Dockerfile, and resources.
- `grep` for the old token: absent from tracked files and from every commit after the
  `git filter-repo` rewrite; the rewritten HEAD tree matches the pre-rewrite tree.
- Unit tests added for all 9 classic pairs and all 25 Sheldon pairs, typed outcomes,
  canonical phrase checks, exact message formatting, move parsing, chat-lock eviction,
  the `GameFlow` prompt loop, and `testApplication` checks for `GET /healthz`, both
  states of `GET /readyz`, and `GET /openapi.yaml`.
- `./gradlew clean test buildFatJar` passes with Temurin JDK 21 (48 tests); the fat
  jar starts and fails fast with a clear message when `BOT_TOKEN` is missing.
- The `/healthz`, `/readyz`, and `/openapi.yaml` routes were smoke-tested against a
  running fat jar and against the deployed Render service.
- The logback configuration is tested with a fake bot token in a message and a stack
  trace; the output contains `bot<redacted>` and not the token. The timeout filter is
  tested with direct, wrapped, and unrelated exceptions.
- Render deploy `dep-dbm7vl942hec73dtvg3g` (commit `a93437c`) reached **Live** and
  `/healthz` returned `200 OK`; later commits followed the same deploy check.
- Runtime smoke test of the Telegram path requires a valid token and was not
  performed.
