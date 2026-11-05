# Known issues and findings

Status as of 2026-11-04 against `main` plus the remediation change. This document is
both a changelog of the review and a list of what still needs owner action.

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

## Still requires owner action

### Rotate the leaked token (high priority)

The token that used to be hardcoded has been purged from every commit, but a token
that was ever committed must be treated as compromised. Forks, existing clones, and
caches may retain it.

1. Revoke the token in [@BotFather](https://t.me/BotFather) and issue a new one.
2. Store it as the GitHub secret: `gh secret set BOT_TOKEN`.
3. Run `gh workflow run deploy-render.yml` to update the Render environment variable
   and redeploy.
4. Verify `/healthz` and send `/start` to the bot.

See [`DEPLOYMENT.md`](DEPLOYMENT.md) for the full setup (Render API key and service
ID) and the rotation runbook. History is already purged; do not rewrite it again
without explicit approval and coordination with everyone who has a clone.

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
- `grep` for the old token: absent from tracked files and from every commit after the
  `git filter-repo` rewrite; the rewritten HEAD tree matches the pre-rewrite tree.
- Unit tests added for all 9 classic pairs and all 25 Sheldon pairs, plus canonical
  phrase checks, and a `testApplication` check for `GET /healthz`.
- `./gradlew clean test buildFatJar` passes with Temurin JDK 21; the fat jar starts
  and fails fast with a clear message when `BOT_TOKEN` is missing.
- The `/healthz` route was smoke-tested against a running fat jar.
- Runtime smoke test of the Telegram path requires a valid token and was not
  performed.
