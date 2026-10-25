---
title: 'CI with the 500-line check'
type: 'chore'
ticket: '3'
created: '2026-10-25'
status: 'built'
baseline_revision: 'f034ae7109489b95097693cb4c452273e6935926'
route: 'oneshot'
route_source: 'auto'
risk: 'low'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Nothing enforces AD-17's 500-line limit per `.kt` file, and no CI runs on push or PR. The spine's Operations section says GitHub Actions runs the build, unit tests, lint and the 500-line check on every push and PR.

**Approach:**
- **Line check:** in the root `build.gradle.kts` (never `app/build.gradle.kts`, which story 1.2 owns), apply the `base` plugin and register a `checkKotlinFileLength` task.
  - It scans every `**/src/**/*.kt` under the repo, excluding `build/` folders.
  - It fails with a list of each file over 500 lines and its line count.
  - Its inputs are declared and it doesn't touch `project` at execution time, so it works with the configuration cache.
  - `check` depends on it, so a root `./gradlew check` and `./gradlew build` both run it.
- **Workflow:** add `.github/workflows/ci.yml`.
  - Triggers: `push` and `pull_request` on all branches.
  - One `ubuntu-latest` job: `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 21, which satisfies "JDK 17+" and the Java 21 test toolchain from story 1.1), and `gradle/actions/setup-gradle@v6` (caching and wrapper validation).
  - It runs `./gradlew build`, which covers assemble, unit tests, lint and the length check.
  - On failure, it uploads the test and lint reports with `actions/upload-artifact@v7`.
  - A concurrency group cancels superseded runs on the same ref.
  - It needs no secrets; release builds stay unsigned in CI.
- **Docs:** `CLAUDE.md` and `.github/copilot-instructions.md` describe CI and the `checkKotlinFileLength` task.

</frozen-after-approval>

## Implementation Notes

Oneshot route: about 60 lines in the root build file, one new workflow file and two docs.

- Action majors were checked against their latest releases on 2026-10-25: checkout v7.0.1, setup-java v6.1.0, gradle/actions v6.4.0, upload-artifact v7.0.2.
- The task captures `rootDir` and the limit in local vals, so `doLast` never touches `project`, which keeps it configuration-cache safe.
- Workflow permissions are `contents: read`, with a 45-minute timeout.
- Verified locally:
  - `./gradlew build --dry-run` includes `:checkKotlinFileLength` and the lint and unit-test tasks.
  - `./gradlew check` passes.
  - With a temporary 501-line `domain/.../TooLongTemp.kt`, the build fails with "Kotlin files over 500 lines (AD-17): domain/src/main/kotlin/com/shashi/expensetracker/domain/TooLongTemp.kt: 501 lines". The file was then deleted.

## Verification

**Commands:**
- `./gradlew build --dry-run`: expected to list `checkKotlinFileLength`, the lint tasks and the unit-test tasks.
- `./gradlew check`: expected to pass on the current tree.
- With a temporary 501-line `.kt` file under `domain/src/main/kotlin`, `./gradlew check`: expected to fail and name the file with 501 lines. Delete the temporary file afterwards.
- After pushing, the PR's CI run: expected to finish green (`gh pr checks`).

## Review Triage Log

### Pass 1 (2026-10-25, quick)

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `val … by tasks.registering` is deprecated on Gradle 9.6 and is removed in Gradle 10 | medium | patch | The reviewer reproduced the warning with `--warning-mode all`. Switched to `tasks.register("checkKotlinFileLength")`, and re-running with `--warning-mode all` shows no deprecation. |
| 2 | `fileTree(rootDir)` also scans `.claude/worktrees/` copies, `.git/` and tooling folders | medium | patch | Real: worktree copies contain `*/src/**/*.kt`. The scan now covers only each subproject's `src` folder. A 501-line file in `app/src/main/kotlin` still fails `check`. |
| 3 | `push` plus `pull_request` runs CI twice for each commit on a PR branch | low | defer | Real cost only; both triggers pass. The frozen intent asks for push on all branches, and sharing a concurrency group would cancel one run and show it as cancelled. |

