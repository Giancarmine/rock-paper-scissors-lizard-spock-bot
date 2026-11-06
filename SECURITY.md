# Security policy

## Supported versions

The latest commit on `main` is the only supported version. The bot is deployed
continuously from `main`; older commits and forks are not maintained.

## Reporting a vulnerability

Please report security issues **privately** through GitHub:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability** (GitHub private vulnerability reporting).

If private reporting is unavailable, contact the maintainer
[@Giancarmine](https://github.com/Giancarmine) through GitHub instead of opening a
public issue.

Include:

- the affected commit or deployment;
- reproduction steps or a proof of concept;
- the impact you expect;
- any suggested fix.

**Do not include a real bot token, `RENDER_API_KEY`, or personal data in the report.**
Use placeholders and redact URLs of the form `https://api.telegram.org/bot<id>:<secret>/...`.

## What is in scope

- Leakage of the Telegram bot token or Render API key.
- Bypassing the token redaction in logs or stack traces.
- Unauthorized access to the HTTP endpoints or the deploy workflow.
- Dependency vulnerabilities that affect the shipped image.
- Injection or denial-of-service bugs reachable through a Telegram update.

Out of scope: behaviour that requires the bot token itself, Telegram platform
outages, Render free-plan cold starts, and missing features already listed in
[`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md).

## If a token is exposed

Treat it as compromised immediately:

1. Revoke it in [@BotFather](https://t.me/BotFather) and issue a new token.
2. Update the GitHub secret: `gh secret set BOT_TOKEN`.
3. Deploy with `gh workflow run deploy-render.yml`.
4. Confirm `/healthz` returns `OK`, `/readyz` returns `READY`, and the bot answers
   `/start`.
5. Check the logs for `bot<redacted>` and no plaintext token.

Do not rewrite git history unless the maintainer explicitly asks; rotation matters
more than history.

## Response

This is a volunteer project without a guaranteed SLA. Reports are acknowledged as
soon as possible, and fixes are released through the normal pull-request workflow.
