---
title: 'Roborazzi screenshot harness for the shell'
type: 'chore'
ticket: '6'
created: '2026-10-25'
status: 'in-progress'
baseline_revision: 'aa6421919514b4eb2e48df896323d93c12434eb4'
route: 'full'
route_source: 'auto'
risk: 'low'
review: ''
review_source: ''
lenses_ran: []
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Nothing catches visual regressions. The spine's Testing convention says key screens get Roborazzi screenshots, and later epics need a ready-made harness to add their screens to.

**Approach:**
- Apply the Roborazzi plugin to `:app` and render with Robolectric native graphics.
- Commit baselines of the five-tab shell (on Home) in four variants: light compact, dark compact, light compact at 1.75x font, and light at 600dp width (rail).
- Make CI verify them. A deliberate visual change must fail `./gradlew verifyRoborazziDebug`.
- Document how a later screen adds its own screenshots.

## Boundaries & Constraints

**Always:**
- Versions come only from the catalog: the plugin and `roborazzi`, `roborazzi-compose` and `roborazzi-junit-rule` at 1.76.0.
- Baselines live in `app/src/test/screenshots/` and are committed.
- Every screenshot test uses `@GraphicsMode(NATIVE)` and the real `MainActivity` through Hilt, so the shell renders exactly as it ships.
- Each variant comes only from Robolectric `@Config` qualifiers and font scale: `w360dp-h800dp`, `+night`, `fontScale = 1.75f`, `w600dp-h900dp`. No test-only code goes into the shell.
- Baseline names are `<screen>_<variant>.png`, for example `shell_home_light.png`.
- CI runs `verifyRoborazziDebug` as part of its build command, and the existing tests keep passing.

**Never:**
- Snapshot tests for any screen other than the shell.
- Paparazzi, or a second screenshot tool.
- Visual changes to the app.
- Committing a baseline that was recorded from a failing build.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Record | `./gradlew recordRoborazziDebug` | 4 PNGs in `app/src/test/screenshots/` | No error expected |
| Verify clean | `./gradlew verifyRoborazziDebug` | Passes | No error expected |
| Visual change | Temporarily change one color | Verify fails and writes a `_compare` diff image | Revert the change |
| Plain tests | `./gradlew test` | Screenshot tests run without recording or failing on a missing baseline | No error expected |

</frozen-after-approval>

## Code Map

- `app/build.gradle.kts`: apply `alias(libs.plugins.roborazzi)` and add the three test libraries. Set `roborazzi { outputDir.set(file("src/test/screenshots")) }`. Add the `robolectric.pixelCopyRenderMode=hardware` system property to the existing `tasks.withType<Test>` block, which already sets Java 21 and the add-opens flags.
- `app/src/test/kotlin/com/shashi/expensetracker/navigation/AppShellTest.kt`: shows how to launch `MainActivity` with `createAndroidComposeRule` and qualifiers, and how to wait for `screen_home`. Reuse that pattern.
- `app/src/test/resources/robolectric.properties`: `sdk=36`. Keep it.
- `.github/workflows/ci.yml:31`: `./gradlew build`. Change it to `./gradlew build verifyRoborazziDebug`. Reports are already uploaded on failure, so add `**/build/outputs/roborazzi/` to the artifact paths.

## Tasks & Acceptance

**Execution:**
- [x] `app/build.gradle.kts`: the plugin, libraries, output dir and render-mode property.
- [x] `app/src/test/kotlin/com/shashi/expensetracker/screenshot/Screenshots.kt`: a small helper. `captureScreen(name)` waits for idle and calls `onRoot().captureRoboImage("src/test/screenshots/$name.png")`. Its KDoc says how a later screen adds tests.
- [x] `app/src/test/kotlin/.../screenshot/ShellScreenshotTest.kt`: four tests, one per variant, each launching `MainActivity`, waiting for `screen_home`, and capturing.
- [x] `app/src/test/screenshots/shell_home_{light,dark,font175,wide}.png`: record these, then open each one to check it shows the shell (pill or rail) and isn't blank.
- [x] `.github/workflows/ci.yml`: add the verify task and the roborazzi artifact path.
- [x] `CLAUDE.md`, `.github/copilot-instructions.md`: the record, verify and compare commands, and where the baselines live.

**Acceptance Criteria:**
- Given the committed baselines, when CI runs on the PR, then `verifyRoborazziDebug` passes on Linux.
- Given a temporary one-token color change, when `./gradlew verifyRoborazziDebug` runs, then it fails and writes a compare image. Reverting the change makes it pass again.

## Implementation Notes

- The root `build.gradle.kts` also declares `alias(libs.plugins.roborazzi) apply false`, matching how the other plugins are declared.
- The dark variant uses the method-level qualifier `+night`, which Robolectric appends to the class-level `w360dp-h800dp`. The font variant is `@Config(fontScale = 1.75f)` on top of the class qualifiers.
- Changing `Primary` (Verification step 3) failed only `ThemeTokensTest`, because `Primary` isn't painted anywhere in the shell. The visual check used `GradientStart` (the active pill/rail item) instead. `verifyRoborazziDebug` then failed `ShellScreenshotTest` and wrote `shell_home_{light,font175,wide}_compare.png` to `app/build/outputs/roborazzi/` (dark uses `GradientStartDark`, so it correctly passed). After reverting, verify passes.
- Plain `testDebugUnitTest` with the baselines folder deleted passes and writes nothing, so a missing baseline doesn't break plain test runs.
- Baselines were recorded on macOS. PR #7 CI (ubuntu-latest) passed `build verifyRoborazziDebug` with them, so no Linux re-record was needed.
- Observed in `shell_home_font175.png`: at 1.75x font, the active pill label truncates to "Ho…". This is existing app behavior, captured as it ships (no visual changes were in scope).

## Plan Change Log

## Review Triage Log

## Design Notes

**Cross-OS rendering:** Robolectric native graphics can anti-alias text slightly differently on macOS and Linux. Baselines are recorded locally first. If the PR's CI verify fails only on font-edge diffs:
1. Re-record on Linux by running `recordRoborazziDebug` in a one-off CI step.
2. Download the artifact.
3. Commit those PNGs.

That makes Linux CI the source of truth. Don't raise the change threshold above 0 to hide this. Record what happened in Implementation Notes.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout (`timeout` isn't installed on this Mac); never leave a build unattended.

**Commands:**
- `./gradlew recordRoborazziDebug`: expected to write 4 PNGs.
- `./gradlew verifyRoborazziDebug`: expected to pass.
- Temporarily change `Primary` in `Color.kt`, run `./gradlew verifyRoborazziDebug` (expected to fail), then revert.
- `./gradlew build`: expected to pass.
- After the push, `gh pr checks`: expected green.
