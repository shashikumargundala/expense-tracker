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
