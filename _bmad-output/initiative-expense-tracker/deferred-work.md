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
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-roborazzi-screenshot-harness-for-the-shell-plan.md`
  summary: The active pill label is cut off at 1.75x font on a 360dp screen. This is verified for "Home" (`shell_home_font175.png` shows "Ho…") and confirms part of the 1.5 truncation entry above, whose 320dp and longer-label ("Expenses", "Settings") checks are still open.
  evidence: The 1.6 baseline at `fontScale = 1.75f` on a `w360dp` screen shows the active Home label ellipsized. At font scale 1.0 on 360dp, "Home" fits (`shell_home_light.png`). A fix, such as icon-only above a font-scale threshold or a wider active item, will change the `font175` baseline.

## Resolved in story 1.8

Plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-refactor-sweep-plan.md`. One line per entry above, in order.

- Add a test that `GetGreetingUseCase` trims a padded name: **resolved**. Added `trims a padded name in the greeting` to `GetGreetingUseCaseTest`.
- Rotate or restrict the old Firebase API key (project `silent-charge-85xj8`): **carried**. It stays open as the owner's action in the Google Cloud console; no code change can revoke it.
- Document the Java 21 `:app` test toolchain in `CLAUDE.md` and the Copilot instructions: **resolved**. Both Project paragraphs now say so, including that foojay downloads it.
- Document the foojay plugin version as the exception to the catalog rule: **resolved**. Added to the dependencies convention in both files.
- Fix the self-reference on line 9 of `.github/copilot-instructions.md`: **resolved**. It now names `CLAUDE.md`, and `CLAUDE.md` names the Copilot file.
- The "Release builds will be unsigned" warning prints on every build: **resolved** by rewording both files to say it's logged whenever Gradle configures the build without the signing variables (and a configuration-cache hit hides it). The build logic is unchanged.
- Duplicate CI runs on PR branches: **resolved**. `.github/workflows/ci.yml` triggers on `pull_request` only (user's decision, 2026-10-25); both doc files say "every PR".
- Dark-mode `onPrimary` fails AA: **resolved**. New DESIGN.md token `on-primary` (#FFFFFF / #171021) maps to `onPrimary` (and `onSecondary`/`onTertiary`), about 10:1 on dark primary. `on-gradient` stays white as `ExtendedColors.onGradient`. `ThemeTokensTest.darkOnPrimaryAndBadgeTextMeetAA` asserts the ratio.
- Pass `scrimColor = MaterialTheme.colorScheme.scrim` on sheets: **carried** as the `scrimColor = colorScheme.scrim` convention in `CLAUDE.md` and `.github/copilot-instructions.md`, for whichever story adds the first bottom sheet. No sheet exists yet.
- Inbox nav badge dark-mode contrast: **resolved**. New DESIGN.md token `on-exceeded` (#FFFFFF / #171021) maps to `onError`, about 6.9:1 on dark exceeded; `NavBadge` text follows. Asserted by the same test.
- Active pill label cut off on narrow phones or at 1.75x font: **resolved**. The active item, pill and rail, shows its icon only above 1.3x font or when its measured label doesn't fit its target width (`showActiveNavLabel`), so it never ellipsizes. The new `shell_expenses_w320.png` shows icon-only Expenses at 320dp; `PillNavTest.activeItemIsIconOnlyAtLargeFontButTalkBackReadsLabel` checks that TalkBack still reads "Expenses". A device check wasn't run.
- `shell_home_font175.png` shows "Ho…": **resolved** by the same rule; the baseline is re-recorded as icon-only Home.
- source_plan: `_bmad-output/initiative-expense-tracker/epic-platform-baseline/story-refactor-sweep-plan.md`
  summary: Reword the Screenshots note in `CLAUDE.md` and `.github/copilot-instructions.md`, which says "today only the shell on Home" although `shell_expenses_w320` exists.
  evidence: `ShellScreenshotTest` now covers Home in four variants plus Expenses at 320dp. Its KDoc is correct; only the two agent-context files are stale.
