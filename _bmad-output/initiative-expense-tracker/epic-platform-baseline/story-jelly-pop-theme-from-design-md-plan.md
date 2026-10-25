---
title: 'Jelly Pop theme from DESIGN.md'
type: 'feature'
ticket: '4'
created: '2026-10-25'
status: 'built'
baseline_revision: 'f034ae7109489b95097693cb4c452273e6935926'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app renders in a plain `MaterialTheme`. Every later screen needs DESIGN.md's Jelly Pop tokens (colors, type, shapes, spacing, glass, aurora) and a reduce-motion signal that each animation can read.

**Approach:** Build `ExpenseTrackerTheme` in `:app` from DESIGN.md's front matter.
- It follows the system light or dark setting, with dynamic color off.
- Tokens that have an M3 role map onto `ColorScheme`. The rest go in an extended-colors `CompositionLocal`.
- It adds typography, shapes, spacing, a glass surface with the API 24–30 fallback, the aurora background, and `LocalReduceMotion` driven by the system "Remove animations" setting.
- A test checks every color token against DESIGN.md itself.

## Boundaries & Constraints

**Always:**
- **Source of values:** every value comes from DESIGN.md's front matter (colors lines 17–95, typography 96–140, rounded 141–146, spacing 147–156). The M3 role mapping follows DESIGN.md Colors (lines 301–313).
- **Alpha order:** DESIGN.md hex is `#RRGGBBAA`, alpha last. Convert it to Compose's `0xAARRGGBB` correctly.
- **Font:** the M3 default family, with no custom font. Use `FontWeight(650)` where DESIGN.md says 650. `letterSpacing` is in `.em`, and `overline` is uppercase in use.
- **Reduce motion:** comes only from the system setting (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`), re-read on resume. There's no in-app toggle (EXPERIENCE.md Motion).
- **Glass fallback:** on API 24–30, glass is opaque `surfaceContainer` with the same 1dp edge and radius (DESIGN.md line 364).
- Every theme file stays at 500 lines or fewer, and existing testTags are kept.
- Decision (2026-10-25): glass uses no backdrop-blur library. On API 31+, it's a translucent fill and 1dp edge over the aurora. Content scrolling underneath isn't blurred. No new dependency.

**Never:**
- Material You or dynamic color.
- An in-app theme or motion toggle.
- Components beyond glass and aurora: the pill nav, rings, FAB and rows come with their screens.
- Navigation or edge-to-edge (story 1.5).
- Roborazzi (story 1.6).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Light | System in light mode | `colorScheme` and extended colors equal DESIGN.md's light tokens | No error expected |
| Dark | System in dark mode | They equal the `-dark` tokens | No error expected |
| Translucent token | `glass` `#FFFFFFB8` | `Color(0xB8FFFFFF)` | No error expected |
| Old API | API 30 | Glass draws opaque `surfaceContainer` plus the 1dp edge, with no blur | No error expected |
| Animations off | `ANIMATOR_DURATION_SCALE` = 0 | `LocalReduceMotion.current == true` | Setting unreadable → false |

</frozen-after-approval>

## Code Map

- `app/src/main/kotlin/com/shashi/expensetracker/MainActivity.kt`: wraps content in `MaterialTheme`. Switch it to `ExpenseTrackerTheme`.
- `app/src/main/kotlin/com/shashi/expensetracker/greeting/GreetingScreen.kt`: the preview uses `MaterialTheme`. Switch it.
- `app/src/test/kotlin/.../greeting/GreetingScreenTest.kt`: wraps content in `MaterialTheme`. Switch it.
- `app/src/main/res/values/colors.xml`: unused template colors, which lint flags. Replace them with the canvas colors only.
- `app/src/main/res/values/themes.xml`: `Theme.ExpenseTracker` with parent `android:Theme.DeviceDefault.NoActionBar`. Set `windowBackground` to the canvas color, and add `values-night/` so launch doesn't flash.
- `app/build.gradle.kts`: needs no change. Compose, Material 3 and Robolectric are already present. It's owned by 1.2.
- `_bmad-output/.../ux-expense-tracker/DESIGN.md`: the token source that the test parses (`../_bmad-output/...` from the `:app` project dir).

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/kotlin/com/shashi/expensetracker/ui/theme/Color.kt`: add the light and dark token values with DESIGN.md names, as Kotlin `Color`s.
- [x] `.../ui/theme/ExtendedColors.kt`: an immutable `ExtendedColors` class (gradient, glass, glass edge, nav glass, aurora 1–3, warning, pending sync, inbox, five category color/tint pairs, edge, scrim), plus `LocalExtendedColors`.
- [x] `.../ui/theme/Type.kt`: an M3 `Typography` from the type scale, using each style's M3 slot from DESIGN.md. Also add a `TextStyle.tabularNumbers()` helper (`fontFeatureSettings = "tnum"`).
- [x] `.../ui/theme/Shape.kt` and `Spacing.kt`: the `rounded` scale as M3 `Shapes` plus named radii, and a `Spacing` object in dp.
- [x] `.../ui/theme/Glass.kt`: `Modifier.glassSurface(shape, navStyle = false)`. Keep a pure `supportsBlur(sdkInt)` decision for testing. On API 31+, draw the translucent token fill (no backdrop library, per the Decision). On API 24–30, draw opaque `surfaceContainer`.
- [x] `.../ui/theme/Aurora.kt`: an `AuroraBackground` composable, three radial glows over the canvas that work on every API.
- [x] `.../ui/theme/Motion.kt`: `LocalReduceMotion` and a reader for the system setting.
- [x] `.../ui/theme/Theme.kt`: `ExpenseTrackerTheme(darkTheme = isSystemInDarkTheme())`, which provides `colorScheme` (roles mapped per DESIGN.md, with unmapped roles derived and listed in a comment), typography, shapes, `LocalExtendedColors` and `LocalReduceMotion`.
- [x] `MainActivity.kt`, `GreetingScreen.kt`, `GreetingScreenTest.kt`, `res/values*/`: switch to the new theme and update the window background.
- [x] `app/src/test/kotlin/.../ui/theme/ThemeTokensTest.kt`: a Robolectric Compose test. For light and dark, it renders the theme and compares every DESIGN.md color token (parsed from the file's front matter) against the theme value. It fails on any token the theme doesn't expose.
- [x] `.../ui/theme/GlassAndMotionTest.kt`: covers `supportsBlur` at API 30 and 31, the alpha conversion, and `LocalReduceMotion` with the animator scale at 0 and 1.

**Acceptance Criteria:**
- Given DESIGN.md's front matter, when `ThemeTokensTest` runs, then all 37 light and 37 dark color tokens match (corrected from 38: DESIGN.md has 20 base, 10 category and 7 derived tokens per mode), and a changed hex in Kotlin makes the test fail.
- Given the debug app, when it launches in light and in dark, then the greeting shows on the cream or grape-black canvas with no white launch flash. This is a manual check.

## Implementation Notes

- Token count: DESIGN.md's front matter has 37 light and 37 dark color tokens, not 38. Each mode has 20 base, 10 category and 7 derived tokens; the plan miscounted. `ThemeTokensTest` asserts 37 per mode and light/dark name parity. It also fails on any token missing from its resolver map.
- Mutation check: changing `InboxDark` to `0xFF6366F2` failed `darkThemeMatchesEveryDarkToken` with `inbox: DESIGN.md #6366F1`. The change was reverted.
- Glass: `glassFill(sdkInt, navStyle, ...)` is a pure helper next to `supportsBlur`, so the API 30 fallback is unit-tested without a device. `glassSurface` uses `Modifier.composed`.
- Theme: `ExpenseTrackerTheme(darkTheme, reduceMotion = rememberReduceMotion(), content)`. `rememberReduceMotion` re-reads the setting on `ON_RESUME` via `LifecycleEventEffect`.
- Unmapped M3 roles beyond Design Notes are derived and listed in the comment at the top of `Theme.kt`. They include secondary/tertiary = primary, and inverseSurface = on-surface with inverseOnSurface = canvas, matching the snackbar spec.
- `Shapes.extraSmall` has no token. It uses `rounded.sm` (12dp), so nothing renders with sharp corners.
- Typography styles that DESIGN.md gives no letterSpacing use `0.em`, replacing M3's default tracking. `amount` is exposed as `AmountTextStyle`, because Body Large stays `body`.
- Window background: `values/colors.xml` and `values-night/colors.xml` define `surface_canvas`. `themes.xml` keeps its parent and sets `android:windowBackground`.

## Plan Change Log

- 2026-10-25: AC token count corrected in the test from 38 to 37 per mode to match DESIGN.md. The AC text is left as written; a human should confirm.

## Review Triage Log

### Pass 1 (2026-10-25, quick)

Verdicts: high 0, medium 3, low 2, false 0, rejected 1 (plan edit). Routes: patch 3, defer 2, reject 1.

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | Dark `onPrimary` white on `primary` #C4B5FD is about 1.85:1, and derived `onSecondary`/`onTertiary` repeat it, so default M3 filled controls fail AA in dark | medium | defer | Real, but the root cause is DESIGN.md's own mapping (on-gradient → onPrimary), which only holds on the gradient. Fixing it means changing the spec, which is the user's decision. No current screen draws a primary control. |
| 2 | Derived `onErrorContainer = exceeded` is #E11D48 on #FFF1F3, about 4.3:1 in light, below AA | medium | patch | Real, and it was the implementer's derivation. Set to `onSurface`. |
| 3 | M3 `ModalBottomSheet` applies `scrim.copy(alpha = 0.32f)`, overriding the token's 45%/65% alpha | low | defer | Real for future sheets; the theme can't fix it. Sheet callers (epic 3) must pass `scrimColor = colorScheme.scrim`. |
| 4 | Amount styles don't include `tnum`, although DESIGN.md says all amounts do | medium | patch | Real: `Type.kt` lines 16–20. Added `fontFeatureSettings = "tnum"` to both styles. |
| 5a | The test comment gives the wrong reason for 38 → 37 | low | patch | Real (`ThemeTokensTest.kt:86-87`). Reworded to say the plan miscounted (20 + 10 + 7). |
| 5b | The plan's AC, Change Log and test disagree about who changed the token count | — | reject | The fix is editing this plan. Main session note: DESIGN.md has 37 + 37 tokens (the implementer recounted and the reviewer confirmed), and the main session corrected the AC wording outside the frozen block. |

## Design Notes

**Unmapped M3 roles:**
- `onBackground` = on-surface.
- `primaryContainer` = indicator.
- `onSecondaryContainer` = on-surface.
- `onPrimaryContainer` = on-surface.
- `onError` = on-gradient.
- `surfaceContainerLowest`/`Low`/`High` = surface-container.
- `surfaceContainerHighest` = ring-track.
- `outline` = on-surface-variant.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout (`timeout` isn't installed on this Mac); never leave a build unattended.

**Commands:**
- `./gradlew :app:testDebugUnitTest`: expected to pass, including `ThemeTokensTest` and `GlassAndMotionTest`.
- `./gradlew build`: expected to pass, with lint reporting no new `UnusedResources` warnings.

**Manual checks:**
- Launch on a device in light and dark mode. The canvas is #FFF7F0 / #171021 and there's no white flash.
