---
title: 'Sign-in screen per the design'
type: 'feature'
ticket: '5'
created: '2026-11-13'
status: 'built'
baseline_revision: '0877b87c60dc3e7a94a5e4d3989dd5e200ce48ad'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Sign-in is a placeholder: a headline, an M3 button and a spinner. It has no brand mark, no official Google button and no privacy line, and TalkBack doesn't announce the setting-up text. DESIGN.md says the official button is required for public release.

**Approach:** Rebuild `SignInScreen` from DESIGN.md's Sign-in spec on the existing aurora. It gets a static rings brand mark, "Expense Tracker" at 28sp/700, Google's official Sign in with Google button in a pill (light theme in light mode, dark theme in dark mode), and the lock and privacy line. Progress replaces the button with a gradient ring and "Setting up your Expense Sheet…", announced politely. Cancel stays silent, and failure shows the existing inline error.

## Boundaries & Constraints

**Always:**
- DESIGN.md wins over the mockup where they differ: the official Google button, not the gradient pill (DESIGN.md "Sign-in button", Do/Don't row).
- Keep the tags `screen_signin`, `signin_continue_google`, `signin_progress`, `signin_progress_text` and `signin_error`, and the existing strings and their wording.
- Touch targets are at least 48dp, and every text pair meets WCAG AA in light and dark. Reduce motion turns off the press squash.
- `:app` imports no Google SDK type (AD-1). The button is Compose, with the official "G" bundled from Google's branding assets: one unmodified PNG per theme, because the current logo's blur and conic gradient can't be a VectorDrawable (owner's decision, 2026-11-13).
- No `.kt` file goes over 500 lines; screen pieces go in `account/components/`.

**Never:**
- No change to `SignInViewModel`'s flow, `SignInUseCase` or any `:domain`/`:data` code.
- No hero chart or shared ring component for Home; epic 4 owns those.
- No approximated or redrawn "G" logo.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Idle | `inProgress=false`, no error | Brand mark, title, Google button (`signin_continue_google`), privacy line | none |
| Signing in | `inProgress=true` | Button gone; `signin_progress` container with ring and "Setting up your Expense Sheet…" (live region, polite); privacy line stays | none |
| Cancelled | Gateway returns Cancelled | Back to idle, no message | none |
| Failed | `error=CouldNotSignIn` | Idle plus "Couldn't sign in. Check your connection and try again." (`signin_error`) | Tap retries |
| Not configured | `error=NotConfigured` | Idle plus "Sign-in isn't set up in this build." | none |
| Dark mode | `+night` | Google's dark button theme; AA text | none |
| 320dp, 1.75x font | Narrow and large text | Mark shrinks, title scales less than body, button label wraps, nothing clipped | none |

**Decisions (owner, 2026-11-13):**
1. The "G" ships as Google's own raster (`drawable-nodpi/ic_google_g_{light,dark}.png`), not a vector.
2. The implementer's CLAUDE.md and `.github/copilot-instructions.md` updates for Sign-in are kept.

</frozen-after-approval>

## Code Map

- `app/.../account/SignInScreen.kt` -- `SignInRoute` L30 and stateless `SignInScreen(uiState, contentPadding, onContinue)` L47: a Column tagged `screen_signin` with a headlineMedium title, `CircularProgressIndicator` tagged `signin_progress` plus a `signin_progress_text` Text (L68–75), an M3 `Button` tagged `signin_continue_google`, and `signin_error` text (L84–92). Rebuild the body here.
- `app/.../account/SignInUiState.kt`, `SignInViewModel.kt` -- `inProgress`, `error: SignInError?` (`CouldNotSignIn`, `NotConfigured`); cancel already clears progress with no error. Don't change.
- `app/src/main/res/values/strings.xml` L18–21 -- `signin_continue_google`, `signin_setting_up`, `signin_error_generic` and `signin_error_not_configured`; add `signin_privacy` ("Private: lives in your own Google Sheet").
- `app/.../ui/theme/Aurora.kt` -- `AuroraBackground`; `AppShell.kt` L71–72 already wraps Sign-in in it, so don't add a second one.
- `app/.../ui/theme/ExtendedColors.kt`, `Theme.kt` L81, L148 -- `gradientStart/End`, `glass`, category colours, ring track `surfaceContainerHighest`, accessed through `ExpenseTrackerTheme.extendedColors`. `Color.kt` L59 `GradientEndDark` (#DB2777) is the text-safe pink.
- `_bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md` L171–179 (`hero-chart` geometry the mark mirrors), L407–411 (the Sign-in spec), L132 (`meta` 11sp); `mockups/key-signin.html` (layout, mark layers, 320dp/1.75x behaviour; ignore its gradient button).
- `app/src/test/.../account/SignInGateTest.kt` -- Hilt and `ActivityScenario` gate tests at `w360dp-h800dp`; `FakeAccountModule.kt` L43–50, where `FakeAccountGateway.result` scripts Cancelled or Failure.
- `app/src/test/.../screenshot/Screenshots.kt` L10–33 (`captureScreen` KDoc) and `ShellScreenshotTest.kt` L26–57 (the `+night`, `fontScale` and `w320dp` pattern); baselines go in `app/src/test/screenshots/`. `FakeAccountModule` is signed in by default, so a Sign-in capture clears `FakeSessionRepository.session` first.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/res/drawable-nodpi/ic_google_g_light.png`, `ic_google_g_dark.png` -- the official "G", cropped unmodified from Google's Sign in with Google branding assets (decision 1) -- branding
- [x] `app/src/main/res/drawable/ic_lock.xml` -- Material Symbols "lock" vector -- privacy line
- [x] `app/.../account/components/GoogleSignInButton.kt` -- pill, minimum height 48dp, "G" 20dp at 12dp start padding with 10dp before the label, Roboto Medium 14sp. Light: fill #FFFFFF, 1dp stroke #747775, text #1F1F1F. Dark: fill #131314, 1dp stroke #8E918F, text #E3E3E3 (Google's guideline values). Label wraps; press squash to 0.92 unless reduce motion is on; `Role.Button`; tag `signin_continue_google` -- official button
- [x] `app/.../account/components/BrandRingsMark.kt` -- static decorative Canvas, `contentDescription` null. Five Category-colour outer segments (12dp stroke, butt caps, 2° gaps) on the ring track, an inner 6dp sweep-gradient orbit, and a radial-gradient core with a glass halo. Its size is set by the caller -- brand mark
- [x] `app/.../account/SignInScreen.kt`, `strings.xml` -- weighted centred brand block (mark, then the title at 28sp/700 in `onSurface`), with the action area at the bottom inside `contentPadding`. Idle shows the button; progress shows a `signin_progress` container (gradient ring plus `signin_progress_text`) with `liveRegion = Polite`. Then the error line, then the lock and privacy line in meta. At narrow widths or large font, shrink the mark and scale the title less than body text -- the layout
- [x] `app/src/test/.../account/SignInScreenTest.kt` -- stateless compose tests for idle, progress (button gone, live region set, privacy line shown), each error message, and the button calling `onContinue` -- matrix rows
- [x] `app/src/test/.../account/SignInGateTest.kt` -- cancel returns to idle with no `signin_error` (add one only if no existing test covers it) -- cancel row
- [x] `app/src/test/.../screenshot/SignInScreenshotTest.kt` and baselines -- `signin_light` (w360), `signin_dark` (w360 `+night`) and `signin_font175` (w320, 1.75x), recorded with `recordRoborazziDebug` -- verify line

**Acceptance Criteria:**
- Given the signed-out app, when Sign-in opens in light or dark mode, then the button is Google's official light or dark Sign in with Google button with the "G" logo, and TalkBack reads it as "Continue with Google, button".
- Given TalkBack is on, when the user taps Continue with Google, then TalkBack announces "Setting up your Expense Sheet…" without moving focus.

## Implementation Notes

- The "G" is cropped from Google's 4x "no text, pill" PNGs in `signin-assets.zip` (2026-04): pixels 40–120, giving 80×80, which is 20dp at xxxhdpi. Every variant of the current logo uses a conic gradient in a `foreignObject`, a mask and Gaussian blurs, which VectorDrawable can't draw.
- CLAUDE.md and the Copilot instructions describe the Sign-in components, tags and screenshot test (decision 2).
- Review pass 1 patches: an always-composed `signin_status` live region (empty while idle), the error in an `onErrorContainer`/`errorContainer` pill with an AA ratio test, the press squash only on the drawn content (the hit area stays 48dp), the lock inline with the privacy text, and the title on a neutral `TextStyle`. The Sign-in baselines were re-recorded; `signin_font175` shows the lock next to "Private:".
- Device check (2026-11-13, S23, dark mode): Sign-in shows the rings mark, the title, Google's dark button with the "G" and the privacy line; the owner checked it and approved.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-11-13, quick): 0 high, 3 medium, 2 low

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | The polite live region is on a node that only appears when progress starts, so TalkBack may stay silent (AC 2) | medium | patch | Real: `SettingUp()` is composed only while `inProgress`; Android announces content changes inside an existing live region, and a newly inserted node usually sends only a subtree change. The test checks only that the `LiveRegion` property exists. Fix: an always-composed live-region container whose text changes. |
| 2 | Light-mode error text, `colorScheme.error` #E11D48 at 15sp on canvas #FFF7F0, is about 4.43:1, below AA's 4.5:1 | medium | patch | Real, and lower over the aurora. The story moved this line to `bodyLarge` and requires AA in both themes. Fix: a pair that meets AA in both themes, with a ratio test. |
| 3 | The outer `graphicsLayer` scale shrinks the button's hit area while pressed | medium | patch | Real: a layer transforms hit testing for the modifiers inside it, so `clickable` loses about 13dp per side at 0.92, below the 48dp rule. Fix: keep `clickable`, minimum height and semantics outside the scaled layer. |
| 4 | A wrapped privacy line leaves the lock at the far left with the text centred | low | patch | Real: visible in `signin_font175.png`; the `Row` with `weight(1f, fill = false)` and centred text spreads them apart. Fix: keep the icon next to the text, a direct layout correction. |
| 5 | The title copies `headlineMedium`, which is `DisplayAmountTextStyle` with `tnum` | low | patch | Real: a later change to the amount style would silently change the Sign-in title. Fix: build the title from a neutral base style, a direct correction. |

## Design Notes

The mark mirrors DESIGN.md's `hero-chart` geometry (112dp at 360dp), but it is Sign-in's own component. Epic 4 may lift it into a shared ring when it builds the hero chart. Category segment order follows the mockup: food, shopping, bills, transport, fun.

## Verification

**Commands:**
- `./gradlew :app:testDebugUnitTest` -- expected: pass
- `./gradlew recordRoborazziDebug`, then `./gradlew build verifyRoborazziDebug` -- expected: pass (what CI runs)

**Manual checks (if no CLI):**
- Open each new `app/src/test/screenshots/signin_*.png` and confirm that the official button, "G", mark, title and privacy line render, and nothing is clipped at 320dp/1.75x.
- On the S23 after sign-out, or with app data cleared: Sign-in looks right in light and dark mode.
