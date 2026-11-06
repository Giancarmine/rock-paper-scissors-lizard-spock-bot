# Known issues and findings

Status as of 2026-11-05 against `main` plus the remediation changes. This document
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

## Still requires owner action

### Rotate the leaked token (high priority)

The token that used to be hardcoded has been purged from every commit, but a token
that was ever committed must be treated as compromised. Forks, existing clones, and
caches may retain it.

The current token was also pasted into chat and appeared in Render runtime logs:
tgbotapi includes the request URL (with the token) in error messages. Two layers are
needed to redact it:

- `logback.xml` redacts `bot<id>:<secret>` from messages and stack traces.
- `installJulToSlf4jBridge()` routes tgbotapi's `java.util.logging` records through
  Logback. Without it, those records bypass the pattern and the token leaks.

The token that was already logged must be rotated, and so must any token written to
the logs before a build with the bridge was deployed. Treat the current value as
compromised.

1. Revoke the token in [@BotFather](https://t.me/BotFather) and issue a new one.
2. Store it as the GitHub secret: `gh secret set BOT_TOKEN`.
3. Run `gh workflow run deploy-render.yml` to update the Render environment variable
   and redeploy.
4. Verify `/healthz` returns `OK`, `/readyz` returns `READY`, and send `/start` to the
   bot.
5. Wait for a new `HttpRequestTimeoutException` line, then check that Render logs show
   `bot<redacted>` and no token value. Old log lines keep the value until retention
   expires, so rotate after the bridge is deployed.

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for the full setup (Render API key and service
ID) and the rotation runbook. History is already purged; do not rewrite it again
without explicit approval and coordination with everyone who has a clone.

## Remaining limitations (accepted for now)

- **No persistence or scores.** Every round is independent.
- **No license.** Treat as all rights reserved unless the owner adds one.
- **OpenAPI file is not served.** It is documentation only; if consumers need it
  over HTTP, add a route.
- **No Telegram API retry/backoff.** Network errors propagate to the handler
  coroutine.
- **Idle long-poll timeouts.** tgbotapi 37 sets the HTTP request timeout equal to
  Telegram's long-poll timeout, so an idle bot logs
  `HttpRequestTimeoutException` roughly every 30 seconds. The polling loop retries
  and updates are not lost. The jul-to-slf4j bridge routes these records through the
  Logback redaction pattern so the token stays out of the logs.
- **Render free-plan sleep.** The service stops after 15 minutes without inbound
  traffic. `.github/workflows/keepalive.yml` pings `/readyz` every 5 minutes, but
  GitHub can delay scheduled runs. Updates remain queued in Telegram for 24 hours.
- **Readiness lags a real outage by up to 60 seconds.** The periodic `getMe()` check
  runs every 60 seconds; `/readyz` does not call Telegram on each request.
- **The per-chat mutex map is bounded by active chats.** Entries are reference-counted
  and evicted when the last caller releases an unlocked lock; an idle chat does not
  keep an entry.

## Verification performed

- Source review of all Kotlin files, Gradle files, Dockerfile, and resources.
- `grep` for the old token: absent from tracked files and from every commit after the
  `git filter-repo` rewrite; the rewritten HEAD tree matches the pre-rewrite tree.
- Unit tests added for all 9 classic pairs and all 25 Sheldon pairs, typed outcomes,
  canonical phrase checks, exact message formatting, move parsing, chat-lock eviction,
  the `GameFlow` prompt loop, and `testApplication` checks for `GET /healthz` and both
  states of `GET /readyz`.
- `./gradlew clean test buildFatJar` passes with Temurin JDK 21; the fat jar starts
  and fails fast with a clear message when `BOT_TOKEN` is missing.
- The `/healthz` and `/readyz` routes were smoke-tested against a running fat jar
  and against the deployed Render service.
- The logback configuration is tested with a fake bot token in a message and a stack
  trace; the output contains `bot<redacted>` and not the token.
- Render deploy `dep-dbm7vl942hec73dtvg3g` (commit `a93437c`) reached **Live** and
  `/healthz` returned `200 OK`.
- Runtime smoke test of the Telegram path requires a valid token and was not
  performed.
