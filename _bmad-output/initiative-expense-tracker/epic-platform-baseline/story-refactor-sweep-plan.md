---
title: 'Refactor sweep'
type: 'refactor'
ticket: '8'
created: '2026-10-25'
status: 'built'
baseline_revision: '932f86b5baa0ad84b30f755d7447571bb947c552'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/deferred-work.md'
  - '{project-root}/_bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 1 left 12 entries in `deferred-work.md`: two dark-mode contrast failures, pill-label truncation, duplicate CI runs, doc gaps, one missing test, and two items only the owner or a later epic can act on. The epic isn't done until each one is resolved or recorded. It runs without story 1.7 (user's decision, 2026-10-25).

**Approach:** Fix everything fixable now, using the user's decisions of 2026-10-25. Mark every deferred entry resolved or carried, with where it went.

**Decisions (user, 2026-10-25):**
1. **Dark contrast.**
   - Add DESIGN.md tokens `on-primary` and `on-exceeded`: #FFFFFF in light, surface-canvas-dark #171021 in dark. `onPrimary` and `onError` map to them.
   - `on-gradient` stays #FFFFFF in both modes. It becomes `ExtendedColors.onGradient` and is used for everything drawn on the gradient (the active pill and rail item).
   - The badge text uses `on-exceeded`.
   - Light mode looks exactly as before.
2. **Pill label at large font.** The active nav item, pill and rail, shows icon only when the font scale is above 1.3, or when its label doesn't fit the item's width (measured, so it doesn't flicker). It keeps the gradient fill, and TalkBack still reads the label.
3. **CI** runs on `pull_request` only. Remove the `push` trigger.

## Boundaries & Constraints

**Always:**
- DESIGN.md changes get a dated changelog line saying they're the user's decision. `ThemeTokensTest` keeps parsing DESIGN.md, so the token count becomes 39 per mode.
- Baselines change only where the decisions change pixels. `shell_home_dark` changes only if a dark `onPrimary` or `onError` pixel is on screen. `shell_home_font175` changes to icon-only.
- Re-record only after you've looked at the old and new image and the difference is the intended one.
- Add a `w320dp` screenshot with Expenses active that proves the fit rule.
- `CLAUDE.md` and `.github/copilot-instructions.md` stay in sync. Each line names the other file, never itself.

**Never:**
- Any other visual change, new dependency or new feature.
- Story 1.7's work.
- Rotating the Firebase key. Only the owner can, so that entry stays open as the owner's action.

</frozen-after-approval>

## Code Map

- `ui/theme/Theme.kt:67,92,110`: `onError = onGradient`, and `onPrimary` fed from `OnGradient`/`OnGradientDark`. Remap them to the new tokens and update the derived-roles comment.
- `ui/theme/Color.kt` and `ExtendedColors.kt`: add `OnPrimary(Dark)` and `OnExceeded(Dark)`, plus `onGradient` in `ExtendedColors`.
- `navigation/PillNav.kt:114` and `AppNavRail.kt`: the active item's content is `colorScheme.onPrimary`. Switch it to `ExtendedColors.onGradient`, then add the icon-only rule (`LocalDensity.current.fontScale > 1.3f`, or `rememberTextMeasurer` width > available).
- `navigation/components/NavBadge.kt:45`: text is `onError`. Once remapped, `onError` is `on-exceeded`. Update the comment.
- `app/src/test/.../ui/theme/ThemeTokensTest.kt:35`: `"on-gradient" to s.onPrimary`. Map it to `e.onGradient` and add resolvers for `on-primary` and `on-exceeded`. Update the count to 39.
- `app/src/test/.../navigation/AppShellTest.kt` (`PillNavTest`): add icon-only at `fontScale` 1.75 with TalkBack still reading the label.
- `app/src/test/.../screenshot/ShellScreenshotTest.kt`: add the `w320dp` Expenses-active variant.
- `DESIGN.md`: front matter lines 28 and 67 (add the new tokens next to them), components lines 216 and 218 (badge), prose line 309 (mapping), and the 386 nav prose (the icon-only rule).
- `.github/workflows/ci.yml:3-5`: remove `push:`.
- `domain/src/test/.../GetGreetingUseCaseTest.kt`: add a test that a padded name is trimmed.
- `CLAUDE.md` and `.github/copilot-instructions.md`, lines 9, 30 and 35, plus the Commands notes:
  - the self-reference on line 9;
  - the CI wording, which becomes "every PR";
  - the "unsigned" warning, which prints whenever Gradle configures the build;
  - the Java 21 test toolchain;
  - the foojay exception;
  - a convention that sheets pass `scrimColor = colorScheme.scrim`.

## Tasks & Acceptance

**Execution:**
- [x] DESIGN.md: the new tokens, mapping, badge and nav rule, plus a changelog line.
- [x] Theme: `Color.kt`, `ExtendedColors.kt` and `Theme.kt`.
- [x] Nav: `PillNav.kt`, `AppNavRail.kt` and `NavBadge.kt`.
- [x] Tests: `ThemeTokensTest`, `PillNavTest` and `GetGreetingUseCaseTest`.
- [x] Screenshots: the new 320dp variant. Then re-record: view each changed PNG before committing it.
- [x] `.github/workflows/ci.yml`: PR-only.
- [x] Docs: `CLAUDE.md` and `.github/copilot-instructions.md`.
- [x] `deferred-work.md`: append a "Resolved in story 1.8" block that lists each earlier entry's summary with `resolved` (and how) or `carried` (and to where). Existing entries are not edited.

**Acceptance Criteria:**
- Given dark mode, when you compute contrast, then `onPrimary`/`primary` and badge text on `error` are each at least 4.5:1. A unit test asserts both ratios.
- Given font scale 1.75 or a 320dp screen, when the shell renders, then the active item shows no clipped label (screenshots), and TalkBack reads "Expenses" (test).
- Given this branch's PR, when CI runs, then there's exactly one CI run per commit, and it's green.

## Implementation Notes

- The label rule lives in `navigation/components/NavItemStyle.kt` as `showActiveNavLabel(label, available)`. It measures `labelMedium` with `rememberTextMeasurer` against the item's target width, not the animating one, so the label doesn't flicker during the resize spring. The pill and the rail both call it.
- DESIGN.md had no changelog section, so one was added at the end, and front matter `updated` moved to 2026-10-25. Line 350 (narrow and large text bullet, "the nav shows only the active item's label") was also reworded to point to the Pill nav rule, so the two don't contradict each other.
- The contrast test is `ThemeTokensTest.darkOnPrimaryAndBadgeTextMeetAA`.
- Screenshots: `compareRoborazziDebug` flagged only `shell_home_font175` (now icon-only Home) and the new `shell_expenses_w320`. `light`, `dark` and `wide` are byte-identical to the old baselines.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-10-25, quick)

Verdicts: medium 1, low 4, false 1. Routes: patch 4, defer 1, reject 1.

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | AC 2 wants a test that TalkBack reads "Expenses" at 320dp, but only font-scale 1.75 is tested; the rail's new rule has no test | medium | patch | Real: `PillNavTest` runs at w360dp and font 1.75 only, which short-circuits before the width measurement. Added a w320dp pill test and a 1.75x rail test. |
| 2 | EXPERIENCE.md:105 still says the active item shows its label | low | patch | Real contradiction with the new DESIGN.md rule. Added the exception. |
| 3 | DESIGN.md:220 `pill-nav.active-item` token still says "icon and label" | low | patch | Real. Added the exception. |
| 4 | Scrim entry "carried to `epic-expenses-and-sync`", which holds no record of it | low | patch | Real, and ticket files can't be written. Reworded it to name where it actually lives: the CLAUDE.md and Copilot convention. |
| 5 | CLAUDE.md and Copilot Screenshots note says "only the shell on Home", but there's now an Expenses baseline | low | defer | Real. The fix edits agent-context files, so the triage rule defers it. |
| 6 | AC 3 (one green CI run per commit) can't be checked before a PR exists | false | reject | It's checked on this story's PR before merge. |

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout (`timeout` isn't installed on this Mac); never leave a build unattended. Don't commit, push or open a PR; the main session does that after review. The PR CI check runs then.

**Commands:**
- `./gradlew build verifyRoborazziDebug`: expected to pass after re-recording.
- `gh pr checks <pr>`: expected to show one `build` check, green.

**Manual checks:**
- Every entry in `deferred-work.md` from this epic has a matching line in the "Resolved in story 1.8" block.
