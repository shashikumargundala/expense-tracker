---
type: epic
title: "A clean, buildable, releasable app shell"
parent: initiative-expense-tracker
covers: []
after: []
assignee: ""
risk: medium
---

# A clean, buildable, releasable app shell

## Description

Restructure the generated single-module app into the three-module Clean Architecture of the spine, upgrade the toolchain, and stand up CI, theme and the navigation shell every later epic builds into. The old single-module code is removed, not migrated (brownfield: start fresh).

## Outcome

The developer and agents get a codebase where the architecture rules are enforced by the build; the signal is a green CI run and an installable closed-track build of the new shell.

## Requirements

- B1: Three Gradle modules — `:domain` (pure Kotlin/JVM), `:data`, `:app` — with dependencies pointing inward and feature packages. (architecture spine, AD-1, AD-2)
- B2: Toolchain at the spine's Stack versions, kept in `gradle/libs.versions.toml`, with a Gradle wrapper and JDK that AGP 9.4.1 supports. (spine, Stack) [ASSUMPTION: the exact Gradle and JDK versions are not in the Stack table; the builder verifies them against AGP 9.4.1's requirements.]
- B3: Hilt wires every dependency; `:domain` uses only `javax.inject`. (spine, AD-16)
- B4: `applicationId` and namespace `com.shashi.expensetracker`; manifest `allowBackup=false` and no `GET_ACCOUNTS`. (spine, Operations; brownfield.md, Project setup changes)
- B5: The old single-module code is removed, not migrated; the Secrets plugin and `.env` go; `CLAUDE.md` and `.github/copilot-instructions.md` describe the new structure. (brownfield.md, Start fresh; spine, Consistency Conventions: Config)
- B6: R8 minification for release with keep rules for Moshi and Retrofit models; release signing from environment variables; debug builds signed with the developer's default Android debug key. (spine, Operations)
- B7: GitHub Actions CI on JDK 17 or newer runs build, unit tests, lint and a check that fails the build on any `.kt` file over 500 lines. (spine, AD-17, Operations)
- B8: DESIGN.md's tokens as the Compose theme in light and dark, following the system theme with Material You off, and a reduce-motion setting every animation checks. (DESIGN.md; EXPERIENCE.md, Motion, Accessibility Floor)
- B9: The 5-tab shell — Home, Expenses, Inbox, Budgets, Settings — with the floating pill nav, a navigation rail at 600dp and wider, edge-to-edge, Back returning to Home, an Inbox badge slot, a start destination that can be gated behind sign-in, and one ViewModel per placeholder screen. (spine, AD-15; EXPERIENCE.md, Information Architecture, Responsive & Platform)
- B10: JVM unit tests and Robolectric/Compose UI tests run from the first story; a Roborazzi screenshot harness for later epics. (spine, Consistency Conventions: Testing; epic Decision)
- B11: A release build on the Play closed test track, with the Play App Signing SHA-1 recorded for the release OAuth client. (spine, Operations)

## Done when

1. `:domain` (pure Kotlin), `:data` and `:app` modules build, with dependencies pointing inward and Hilt wiring them (AD-1, AD-2, AD-16).
2. Toolchain matches the spine's Stack table, app ID and namespace are `com.shashi.expensetracker`, and the manifest has `allowBackup=false` and no `GET_ACCOUNTS`.
3. GitHub Actions runs build, unit tests, lint and the 500-line file check on every push and PR, and fails when a `.kt` file exceeds 500 lines (AD-17).
4. The app launches into the 5-tab shell (Home, Expenses, Inbox, Budgets, Settings) with placeholder screens, in DESIGN.md's light and dark themes, with a navigation rail at 600dp and wider; the Roborazzi screenshot harness covers it.
5. A minified (R8) release build signed for Play App Signing is on the Play closed test track.

## Boundaries

The platform baseline (scaffold, toolchain, CI, release path, theme and shell). Not any feature screen's content.

## References

- constraint — _bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md, AD-1, AD-2, AD-15, AD-16, AD-17, Stack, Operations
- brownfield — _bmad-output/initiative-expense-tracker/spec-expense-tracker/brownfield.md, Start fresh and Project setup changes
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md, tokens, Colors, Typography, Components (pill nav)
- design — _bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md, Information Architecture, Responsive & Platform

## Notes

- Decision: owns the Roborazzi screenshot harness for later epics (2026-10-24).
- Decision: tracer bullet is entry 1 — the three modules on the new toolchain with a greeting flowing from a `:domain` use case through a `:data` stub to the screen, run on a device (2026-10-24).
- Decision: after entry 1, three lanes run in parallel — build config (2), CI (3), UI (4, 5, 6); entry 7 follows 2 and 5. Entry 3 keeps its line check in root build logic so it never edits `app/build.gradle.kts`, which entry 2 owns (2026-10-24).
- Decision: the old single-module code is deleted in entry 1; it remains in git history at commit 26e51ad (user's decision, 2026-10-24).
- Decision: debug builds use each developer's default Android debug key; its SHA-1 goes in the debug OAuth client in epic-account-and-sheet (user's decision, 2026-10-24).
- Decision: `READ_SMS` is declared in epic-sms-inbox together with the Play SMS declaration, not here (user's decision, 2026-10-24).
- Decision: Refactor sweep closes the epic as entry 8 (2026-10-24).
- Decision: the owner's Google Play developer account is active (2026-10-28); entry 7 is unblocked. It replaces the earlier unknown about account verification.
- Decision: no Play release for now; signed release APKs are shared directly (story 9) until story 7 can run (user's decision, 2026-10-27).
- Reminder: story 7 (Play closed track) must still be implemented once the owner's Play developer account is verified. Its Play App Signing enrolment reuses story 9's release key.
- Handoff: release key SHA-1 `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25` (`CN=Expense Tracker, O=Expense Tracker`, created 2026-10-27 in story 9) for epic-account-and-sheet's release OAuth client. It stays valid for Play if story 7 enrols this key in Play App Signing. See `docs/RELEASE.md`.
