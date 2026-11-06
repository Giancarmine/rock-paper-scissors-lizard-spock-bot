## Summary

<!-- What changes, and why? Link the issue or known-issue entry when there is one. -->

## Changes

- <!-- Bullet the notable code, test, and documentation changes. -->

## Verification

- [ ] `./gradlew ktlintCheck test buildFatJar`
- [ ] Tested the relevant path locally (`/healthz`, `/readyz`, `/openapi.yaml`, or a
      Telegram command)
- [ ] `actionlint` passes for workflow changes

## Checklist

- [ ] The change stays on Render's free plan and works with Telegram long polling.
- [ ] No token, secret, `.env` file, or unredacted request URL is included.
- [ ] New or changed behaviour has a test.
- [ ] README/docs/AGENTS.md are updated when behaviour or structure changed.
