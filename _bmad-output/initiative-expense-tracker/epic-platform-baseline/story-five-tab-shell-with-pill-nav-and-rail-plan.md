---
title: 'Five-tab shell with pill nav and rail'
type: 'feature'
ticket: '5'
created: '2026-10-25'
status: 'built'
baseline_revision: 'a848534e9adba2304ce705ed4a1d9e8b780f9964'
route: 'full'
route_source: 'auto'
risk: 'medium'
review: 'quick'
review_source: 'pinned'
lenses_ran: ['quick']
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/initiative-expense-tracker/ux-expense-tracker/DESIGN.md'
  - '{project-root}/_bmad-output/initiative-expense-tracker/ux-expense-tracker/EXPERIENCE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The app shows one greeting screen with no navigation. Every later epic adds its screens into the five-tab shell (Home, Expenses, Inbox, Budgets, Settings) that EXPERIENCE.md's Information Architecture defines, and that shell doesn't exist yet.

**Approach:** Build the shell in `:app`.
- A Navigation Compose host with one placeholder screen and ViewModel per tab.
- DESIGN.md's floating glass pill nav, replaced by a navigation rail at 600dp and wider.
- Edge-to-edge, with Back returning to Home.
- An Inbox badge slot, and a start destination that the account epic can gate behind sign-in.
- Home takes over the greeting tracer from story 1.1.

## Boundaries & Constraints

**Always:**
- Tab order, labels and Home as the start tab follow EXPERIENCE.md lines 30–39.
- Pill nav visuals:
  - Glass uses `Modifier.glassSurface(navStyle = true)` from story 1.4, `rounded.full` and a 48dp item height.
  - The active item is about twice the width of the others, with a gradient fill and icon plus label.
  - Inactive items show the icon only and have a TalkBack label (DESIGN.md lines 210–218 and 386).
- The rail at 600dp and wider has the same five items, the gradient active item and the badge (DESIGN.md line 386).
- **Back:** from any other tab, Back returns to Home. From Home, the system handles Back. Each tab keeps its own state (`saveState`/`restoreState`, `launchSingleTop`).
- **Badge:** the Inbox badge slot is an `inboxBadgeCount: Int` that is hidden at 0. It uses `{colors.exceeded}` with on-gradient text, and TalkBack reads it as "N new".
- **testTags:**
  - nav items: `nav_home`, `nav_expenses`, `nav_inbox`, `nav_budgets`, `nav_settings`
  - containers: `pill_nav` and `nav_rail`
  - placeholder screens: `screen_home` … `screen_settings`
  - existing tags are kept.
- Every touch target is at least 48dp. Each ViewModel exposes one immutable `UiState` `StateFlow` and depends only on `:domain` (AD-15).
- Every file stays at 500 lines or fewer, and reduce motion turns off any nav animation.
- Decision (2026-10-25): Home lives in a new `:app`-only screen package `home`. AD-2 gets a note in the spine: "`:app` may have a `home` screen package that composes other features".

**Never:**
- New libraries. No kotlinx-serialization, material3-adaptive or icons-extended: routes are strings and icons are vector drawables.
- Any FAB, snackbar or real screen content. The FAB comes per screen in later epics.
- A sign-in screen (epic account-and-sheet).
- Roborazzi (story 1.6).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Launch | Cold start | Home is selected and `screen_home` shows the greeting | No error expected |
| Tab switch | Tap each `nav_*` | Its `screen_*` shows and that item is active | No error expected |
| Back | On Budgets, press Back | Home is selected | No error expected |
| Wide | Width 600dp or more | `nav_rail` is shown and `pill_nav` is absent | No error expected |
| Badge | `inboxBadgeCount` = 0 or 3 | Hidden at 0; at 3 it shows "3" and reads "3 new" | No error expected |

</frozen-after-approval>

## Code Map

- `app/src/main/kotlin/com/shashi/expensetracker/MainActivity.kt`: currently hosts `GreetingScreen` in `ExpenseTrackerTheme`. Make it the navigation host only (ARCH:222): call `enableEdgeToEdge()`, then draw `ExpenseTrackerTheme { AppNavHost() }`.
- `app/src/main/kotlin/com/shashi/expensetracker/greeting/`: tracer `GreetingScreen`, `GreetingViewModel` and `GreetingUiState`. Move its logic into the Home placeholder and delete the package. `:domain`/`:data` `greeting` stay, because Home still calls `GetGreetingUseCase`.
- `app/src/test/.../greeting/*`, `MainActivityTest.kt`: move these to Home and retarget them from `greeting_text` to `screen_home`. Keep the `greeting_text` tag on Home's greeting.
- `app/src/main/kotlin/.../ui/theme/`: story 1.4's `glassSurface`, `ExtendedColors` (gradient), `LocalReduceMotion`, `Spacing` and `Typography.titleLarge` (screen-title). Reuse them and don't change them.
- `app/build.gradle.kts`: add `implementation(libs.androidx.navigation.compose)` and `implementation(libs.androidx.hilt.navigation.compose)`. Both are already in the catalog. Story 1.2 owns this file but makes no further changes in this epic.

## Tasks & Acceptance

**Execution:**
- [x] `app/build.gradle.kts`: add the two navigation dependencies.
- [x] `.../navigation/Destinations.kt`: a `TopLevelDestination` enum (route, label, icon, testTag) in tab order, plus a `MainGraph` route constant.
- [x] `.../navigation/AppNavHost.kt`: `AppNavHost(startDestination: String = MainGraph, inboxBadgeCount: Int = 0)`. It's a root `NavHost` with a nested `MainGraph` holding the five tabs. This is the gate seam: the account epic adds a sign-in graph and passes the start.
- [x] `.../navigation/AppShell.kt`: a `BoxWithConstraints` that picks `PillNav` below 600dp and `AppNavRail` at 600dp and wider. It owns the navigate-with-`popUpTo(home)` logic and applies insets: content uses `safeDrawing`, and the pill sits above `navigationBars` with 16dp margins.
- [x] `.../navigation/PillNav.kt` and `AppNavRail.kt`: the visuals above. The badge is shared as `NavBadge` in `.../navigation/components/`.
- [x] `app/src/main/res/drawable/ic_nav_*.xml`: five Material Symbols vectors (home, list, inbox, pie chart, tune).
- [x] `.../home/`, `.../expense/`, `.../inbox/`, `.../budget/`, `.../settings/`: one `<Tab>Screen`, `<Tab>ViewModel` and `<Tab>UiState` each. The four non-Home placeholders show their title in the screen-title style. Home shows the greeting.
- [x] Delete `.../greeting/` from `:app` and move its tests.
- [x] `app/src/test/kotlin/.../navigation/AppShellTest.kt`: covers the matrix rows. Wide runs under Robolectric `@Config(qualifiers = "w600dp-h900dp")`, compact under `w360dp-h800dp`. Badge rows render `PillNav` directly.
- [x] `_bmad-output/initiative-expense-tracker/architecture-expense-tracker/architecture-expense-tracker.md`: add the `home` note to AD-2, dated 2026-10-25 and marked as the user's decision.
- [x] `CLAUDE.md`, `.github/copilot-instructions.md`: describe the shell, the navigation packages and how to add a tab screen.

**Acceptance Criteria:**
- Given the debug app on a phone, when the user taps each tab and presses Back, then the right placeholder shows, Back lands on Home, and content isn't hidden under the system bars or the pill. This is a manual check.
- Given `./gradlew build`, when it runs, then it passes, including `AppShellTest`, the moved Home tests and the 500-line check.

## Implementation Notes

- **Icons:** the five `ic_nav_*` vectors (`home`, `list`, `inbox`, `pie_chart`, `tune`) are drawn by hand in the Material Symbols Rounded outline style: a 24dp viewport with 2dp round strokes. They aren't copied path data.
- **`hiltViewModel`:** imported from `androidx.hilt.lifecycle.viewmodel.compose`, which `hilt-navigation-compose` 1.4.0 brings in as an API dependency. The `androidx.hilt.navigation.compose` variant is deprecated.
- **Tests:** `AppShellTest.kt` has two classes.
  - `AppShellTest` drives `MainActivity` with the real Hilt graph and covers launch, tab switch, Back and the wide and compact widths.
  - `PillNavTest` renders `PillNav` directly under a plain `Application`. It covers the badge at 0 and 3, the labels, the 48dp minimum and the 2:1 width ratio, and reduce motion.
- **Badge semantics:** the badge adds no semantics (`clearAndSetSemantics {}`). Each nav item, on both the pill and the rail and whether active or not, has one explicit content description: its label, plus ", N new" while the badge shows ("Inbox, 3 new"). Icons have no description of their own. Counts above 99 show as "99+".
- **Pill widths:** each inactive item takes one share and the active item two, with a 48dp floor. At 360dp that gives 53dp and 107dp.
- **Rail:** a vertical floating glass pill at the start edge, vertically centered. Its items are 80dp wide, and the active item is 56dp tall with its label under the icon.
- **Insets and motion:** content is placed at a stable call site, so crossing 600dp never recreates the `NavHost`. Screens read `contentPadding` through a `State`, so inset changes don't rebuild the graph. Tab cross-fades are 200ms, and reduce motion turns them off.

## Plan Change Log

## Review Triage Log

### Pass 1 (2026-10-25, quick)

Verdicts: high 0, medium 2, maybe-false 1, false 1. Routes: patch 1, defer 2, reject 1.

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | With Inbox active and badged, TalkBack reads "3 new" without "Inbox", because the badge's description overrides the label (pill and rail) | medium | patch | The reviewer's Robolectric probe showed `cd=[3 new] text=[3, Inbox]`. The badge now adds no semantics, and each item has one description from `navItemDescription` ("Inbox, 3 new"). Added `activeInboxWithBadgeReadsLabelPlusNew`, which passes. |
| 2 | The active pill label ("Expenses", "Settings") may be cut off at 360dp or narrower, or at 1.75x font | maybe-false | defer | The arithmetic leaves about 53dp at 360dp. Robolectric text metrics can't measure it, so a device check settles it. Medium if true. |
| 3 | The Inbox badge's white on dark #FB7185 is about 2.5:1, below AA | medium | defer | Real, but the code follows DESIGN.md's badge token (line 218) exactly. The fix needs a DESIGN.md decision, grouped with 1.4's dark `onPrimary`. |
| 4 | The nav ripple isn't gated on reduce motion | false | reject | The system "Remove animations" setting that drives `LocalReduceMotion` sets the animator duration scale to 0, which also stops the platform ripple animations. The app-owned nav animations (size, cross-fade) are gated. |

## Design Notes

- **Active pill label contrast:** white on #EC4899 fails AA (DESIGN.md line 323). The active fill runs from `gradient-start` to #DB2777, the darker pink DESIGN.md names, so the white label passes.
- **Where the routes live:** route strings exist only in `Destinations.kt`. Screens never see `NavController`; they get lambdas.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout (`timeout` isn't installed on this Mac); never leave a build unattended.

**Commands:**
- `./gradlew :app:testDebugUnitTest`: expected to pass, including `AppShellTest`.
- `./gradlew build`: expected to pass, including lint and `checkKotlinFileLength`.

**Manual checks:**
- On a phone: tab through all five tabs, press Back to return to Home, and check that the pill floats above the gesture bar in light and dark.
