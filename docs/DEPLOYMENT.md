# Deployment

The bot is a long-running process that uses Telegram **long polling**. On Render it
runs as a Docker web service; Render requires a bound HTTP port, which the bot
provides with `GET /healthz` (liveness) and `GET /readyz` (Telegram connectivity).

## How deployment works

```text
GitHub Actions (deploy-render.yml)
  ├─ reads secrets.BOT_TOKEN, secrets.RENDER_API_KEY, vars.RENDER_SERVICE_ID
  ├─ PUT /v1/services/{id}/env-vars/BOT_TOKEN   (updates the runtime token)
  ├─ POST /v1/services/{id}/deploys             (rebuilds and redeploys)
  └─ Render builds the Dockerfile and runs the container
```

The token is **never** baked into the image or committed. It is sent to Render at
deploy time and stored as a service environment variable.

## One-time setup

### 1. Create the bot and the Render service

1. Create a bot with [@BotFather](https://t.me/BotFather) and keep the token out of
   the repository.
2. Create a Render **Web Service** from this repository:
   - Runtime: **Docker**
   - Dockerfile path: `Dockerfile`
   - Health check path: `/healthz`
   - Environment variable: `BOT_TOKEN` = your token (initial value; the workflow
     will manage it afterwards)
3. Copy the service ID (`srv-...`) from the Render dashboard URL or service
   settings.

### 2. Create a Render API key

Render Dashboard → Account Settings → API Keys → create a key. It needs permission
to update service environment variables and trigger deploys.

### 3. Configure GitHub

Install the GitHub CLI and run these commands from the repository:

```bash
# The new bot token (prompted; never echoed into shell history)
gh secret set BOT_TOKEN

# The Render API key (prompted)
gh secret set RENDER_API_KEY

# Non-secret identifier, stored as a repository variable
gh variable set RENDER_SERVICE_ID --body "srv-xxxxxxxxxxxxxxxx"
```

You can also add them in GitHub → Settings → Secrets and variables → Actions.

### 4. Deploy

Run the workflow manually:

```bash
gh workflow run deploy-render.yml
```

Or use **Actions → Deploy to Render → Run workflow**. Pushing a tag matching `v*`
also triggers it.

Watch the run with:

```bash
gh run list --workflow=deploy-render.yml
gh run watch
```

## Staying on the free plan

The service runs on Render's **free** plan. Free web services stop after 15 minutes
without inbound traffic; outbound long polling alone does not keep them awake. The
[`Keep alive`](../.github/workflows/keepalive.yml) workflow requests `/readyz` every
5 minutes so the service stays up and keeps processing Telegram updates. Targeting
readiness (not liveness) means the workflow fails when the process is up but its
Telegram connection is dead, instead of keeping a useless process alive.

- One always-on free service stays within Render's monthly instance-hour allowance
  (750 hours; a 31-day month is 744 hours). The allowance is shared across the
  workspace, so budget for any other free services.
- GitHub scheduled workflows can be delayed, especially at high load. If the service
  does sleep, the next ping wakes it; Telegram retains updates for 24 hours, so
  messages are delivered late rather than lost.
- A cold start can take several minutes. The workflow's curl retry budget is 8m45s
  (`4 × 120s` plus `3 × 15s`) and its job timeout is 10 minutes, so a wake-up attempt
  is not cut off. Keep the job timeout above the curl budget if you tune either value.
- `cancel-in-progress` is `false`: a run that is waiting for a sleeping service must
  be allowed to finish, so a newer scheduled run queues instead of cancelling it.
- Update `READINESS_URL` in the workflow if the service URL changes. Remove the
  workflow after moving to a paid plan that does not sleep.
- Scheduled workflows are disabled after 60 days without repository activity; a
  regular commit or manual run re-enables them.

## Token rotation runbook

Use this after a leak or a scheduled rotation:

1. In @BotFather, revoke the old token and generate a new one.
2. Update the GitHub secret:
   ```bash
   gh secret set BOT_TOKEN
   ```
3. Run the deploy workflow:
   ```bash
   gh workflow run deploy-render.yml
   ```
4. Confirm the run succeeds and the Render deploy reaches **Live**:
   ```bash
   render deploys list srv-xxxxxxxxxxxxxxxx -o text
   ```
5. Send `/start` to the bot to verify it responds.
6. Check `https://<your-service>.onrender.com/healthz` returns `OK` and
   `https://<your-service>.onrender.com/readyz` returns `READY` (503 until the bot
   has connected to Telegram).
7. Confirm new Render log lines no longer contain the token. Wait for the next
   `HttpRequestTimeoutException`, then check for `bot<redacted>`. tgbotapi includes
   the request URL (and therefore the token) in error messages, so
   `src/main/resources/logback.xml` redacts `bot<id>:<secret>` and
   `installJulToSlf4jBridge()` routes tgbotapi's `java.util.logging` records through
   Logback. Older log lines keep the previously leaked value until log retention
   expires; revoking the token is what neutralizes them.

No code change or commit is needed for a rotation, but the deployed build must
include the jul-to-slf4j bridge. Rotate only after that build is live; otherwise the
new token is written to the logs again.

## Security notes

- Do not add `BOT_TOKEN` to `Dockerfile`, source files, `.env` committed to git, or
  workflow files. The workflow reads it from `secrets.BOT_TOKEN`.
- The workflow never echoes the token. The `PUT` body is passed through stdin to
  `curl` to keep it out of process arguments where practical.
- GitHub masks secret values in logs, but avoid printing them anyway.
- Runtime logs are written by the app, not by GitHub. tgbotapi includes the Telegram
  request URL (which contains the token) in error messages, so `logback.xml` applies a
  redaction pattern to every message and stack trace. tgbotapi logs through kslog,
  which uses `java.util.logging` on the JVM; `installJulToSlf4jBridge()` installed in
  `Application.kt` routes those records through Logback. Do not remove either the
  pattern or the bridge: without the bridge, JUL records bypass Logback and the token
  is logged in plaintext. If a token still appears in Render logs, rotate it
  immediately and check that the deployed build installs the bridge.
- Render environment groups are not updated by this workflow; it only updates
  service-level variables.

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| `Missing required configuration: BOT_TOKEN` | Repository secret is not set. |
| `Missing required configuration: RENDER_SERVICE_ID` | Repository variable/input is missing. |
| Workflow fails with HTTP 401 | `RENDER_API_KEY` is invalid or revoked. |
| Workflow fails with HTTP 403 | API key lacks the required scopes. |
| Deploy stays queued or fails | Check the Render deploy logs; the service may need a plan/region change. |
| Bot starts but does not answer | The token is invalid/revoked, or the service cannot reach `api.telegram.org`. |
| Bot does not answer after a quiet period | The free service slept; check the `Keep alive` workflow and wake it with `curl https://<your-service>.onrender.com/readyz`. |
| Render logs show `HttpRequestTimeoutException` every 30 seconds | tgbotapi's client timeout equals the long-poll timeout while idle. The token is redacted; the bot keeps polling. |
| `/healthz` returns 404 | The deployed revision predates the health route; redeploy. |
| `/readyz` returns 503 while `/healthz` returns 200 | The process is up but the Telegram connection failed (revoked token, blocked network, or Telegram outage). Check the startup log and Render logs. The bot re-checks every 60 seconds. |
