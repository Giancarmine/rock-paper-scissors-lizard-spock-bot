# Contributing

Thanks for taking a look at the bot. This project is small on purpose: a Kotlin
Telegram bot with a couple of HTTP probes, no database, and no framework beyond Ktor
and `tgbotapi`.

## Before you start

- Read [`AGENTS.md`](AGENTS.md) for the architecture and the hard security rules.
- Read [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the update flow.
- Check [`docs/KNOWN_ISSUES.md`](docs/KNOWN_ISSUES.md) before working on a limitation;
  it may already be explained or intentionally accepted.

## Requirements

- JDK 21 (the build and the Docker image both use it).
- Network access on the first build so Gradle can download dependencies.
- A Telegram bot token only if you want to run the bot locally. Tests do not need one.

## Workflow

`main` is protected by CI and stays deployable. Use a short-lived branch:

```bash
git switch -c feat/short-description
# make changes
./gradlew ktlintCheck test buildFatJar
git commit -m "Add short description"
git push -u origin feat/short-description
gh pr create --fill
```

Wait for CI, then squash-merge. Do not push directly to `main`.

### Branch names

| Prefix | Use for |
| --- | --- |
| `feat/` | User-visible behaviour or a new endpoint. |
| `fix/` | Bug fixes. |
| `test/` | Test-only changes. |
| `docs/` | Documentation, comments, and templates. |
| `chore/` | Build, dependency, and tooling updates. |

### Commit messages

Use the imperative mood and keep the subject under ~72 characters. Conventional
Commit prefixes (`feat:`, `fix:`, `docs:`, `test:`, `chore:`) are welcome but not
required. Explain *why* in the body when the change is not obvious.

## Quality gates

Run these before opening a pull request:

```bash
./gradlew test buildFatJar
```

- **Tests.** New behaviour needs a test. The suite covers both game modes, parsing,
  message formatting, chat locks, `GameFlow`, the HTTP routes, and log redaction.
- **Docker.** Change the `Dockerfile` only if you can build the image locally:
  `docker build -t rpsls-bot .`
- **Workflows.** If you edit `.github/workflows`, run `actionlint`.

## Security rules

1. Never commit, print, echo, or paste a real `BOT_TOKEN`. Use
   `123456789:AAExampleTokenValueUsedOnlyForThisTest` in tests.
2. `.env` is git-ignored; `.env.example` lists variable names only.
3. Do not remove the token redaction pattern in
   `src/main/resources/logback.xml` or the JUL-to-SLF4J bridge. Both are required to
   keep tokens out of Render logs.
4. Report vulnerabilities privately as described in [`SECURITY.md`](SECURITY.md),
   never in a public issue.

## Pull request checklist

- [ ] The change is focused and described in the PR title.
- [ ] `./gradlew ktlintCheck test buildFatJar` passes.
- [ ] New or changed behaviour has a test.
- [ ] No secret, token, or `.env` file is included.
- [ ] Documentation (`README.md`, `docs/`, `AGENTS.md`) is updated when needed.
- [ ] The PR works on Render's free plan; no paid feature is required.
