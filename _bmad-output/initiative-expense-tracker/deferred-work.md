# Deferred work

- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md`
  summary: Add a test that `GetGreetingUseCase` trims a padded name in the success greeting.
  evidence: Only `"Shashi"` and `"   "` are tested. Low value, because the `greeting` tracer is removed in story 1.5 and the stub never pads.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md`
  summary: Rotate or restrict the Firebase API key in the old `firebase-applet-config.json` (project `silent-charge-85xj8`), or delete that Firebase project.
  evidence: The file was deleted in story 1.1, but the key is still readable at `git show 26e51ad:firebase-applet-config.json`. Deleting a file doesn't revoke a key; only the owner can do that in the Google Cloud console.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md`
  summary: Say in `CLAUDE.md` and `.github/copilot-instructions.md` that `:app` unit tests run on a Java 21 toolchain (Robolectric 4.17 SDK 36 sandbox), which foojay downloads automatically.
  evidence: Both files say "JDK 17", while `app/build.gradle.kts` sets a Java 21 `javaLauncher` for `Test` tasks. The README and spine are already updated. The triage rule defers edits to agent-context files.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md`
  summary: Document in `CLAUDE.md` and the Copilot instructions that the foojay plugin version in `settings.gradle.kts` is an exception to the "all versions in the catalog" rule.
  evidence: The settings `plugins {}` block can't use catalog aliases, and `foojay-resolver-convention` is pinned to `1.0.0` there. The Java 21 test toolchain now depends on it.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-three-module-skeleton-launches-on-the-new-toolchain-plan.md`
  summary: Fix `.github/copilot-instructions.md` line 9, which tells readers to keep the file in sync with itself; it should point to `CLAUDE.md`.
  evidence: The line was copied verbatim from CLAUDE.md ("`.github/copilot-instructions.md` holds the same guidance").
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-manifest-signing-and-r8-release-config-plan.md`
  summary: Show the "Release builds will be unsigned" warning only when a release build is requested, or reword CLAUDE.md and the Copilot instructions to say it prints whenever Gradle configures the build.
  evidence: `logger.warn` in `app/build.gradle.kts` runs during configuration, so every `assembleDebug` and `test` without signing variables prints it. A configuration-cache hit hides it even on `assembleRelease`.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-ci-with-the-500-line-check-plan.md`
  summary: Avoid duplicate CI runs on PR branches, for example by limiting `push` to `master`, which needs the user's agreement to change story 1.3's "every push" intent.
  evidence: `.github/workflows/ci.yml` triggers on both `push` and `pull_request`. Their refs differ (`refs/heads/*` and `refs/pull/*/merge`), so the concurrency group doesn't dedupe them, and each commit runs `./gradlew build` twice.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-jelly-pop-theme-from-design-md-plan.md`
  summary: Decide dark mode's `onPrimary`, because DESIGN.md maps on-gradient (#FFFFFF) to `onPrimary`, but white on dark `primary` #C4B5FD is about 1.85:1 and fails WCAG AA for M3 filled buttons, switches and checkboxes.
  evidence: The mapping only holds on the gradient. A fix needs a DESIGN.md change, for example dark `onPrimary` = surface-canvas-dark #171021 (about 10:1), or a rule that primary controls always draw the gradient. `secondary`/`tertiary` derive from `primary` in `Theme.kt` and would follow.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-jelly-pop-theme-from-design-md-plan.md`
  summary: When sheets arrive, pass `scrimColor = MaterialTheme.colorScheme.scrim` so the token's alpha (45% light, 65% dark) applies.
  evidence: M3's `BottomSheetDefaults.ScrimColor` is `colorScheme.scrim.copy(alpha = 0.32f)`, which replaces the token's alpha.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-five-tab-shell-with-pill-nav-and-rail-plan.md`
  summary: Fix the Inbox nav badge's dark-mode contrast. DESIGN.md's badge token (exceeded with on-gradient text) gives white on #FB7185, about 2.5:1, below AA's 4.5:1 for its 11sp text.
  evidence: `NavBadge.kt` draws `onError` on `error`, exactly as DESIGN.md line 218 says. A fix needs a DESIGN.md decision (for example dark ink text on the dark exceeded red), alongside the dark-mode `onPrimary` decision deferred from story 1.4.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-five-tab-shell-with-pill-nav-and-rail-plan.md`
  summary: Check on a device that the active pill label ("Expenses", "Settings") isn't cut off on 360dp and narrower phones, or at 1.75x font. Unverified; medium if true.
  evidence: The arithmetic leaves about 53dp for a 12sp/650 label at 360dp and about 34dp at 320dp, with `maxLines = 1` and ellipsis. Robolectric's text metrics can't measure it. A device check at 320dp and 360dp, at font scale 1.0 and 1.75, settles it.
