---
name: Expense Tracker
description: Private, offline-first expense tracker for Android. Orbit Rings structure (frosted glass, aurora glow, progress rings) in the Jelly Pop palette. Playful and premium, built on Material 3 Compose.
status: final
created: 2026-10-08
updated: 2026-10-08
sources:
  - .memlog.md
  - mockups/direction-orbit-rings.html
  - mockups/direction-jelly-pop.html
  - mockups/key-home.html
  - mockups/key-expenses.html
  - mockups/key-inbox.html
  - mockups/key-budgets.html
  - mockups/key-signin.html
  - mockups/key-settings.html
colors:
  # Light: Jelly Pop palette, copied exactly from direction-jelly-pop.html
  surface-canvas: '#FFF7F0'
  surface-container: '#FFFFFF'
  on-surface: '#2B1838'
  on-surface-variant: '#7A6A86'
  outline-variant: '#F0E4DA'
  edge: '#EADCF0'
  primary: '#6D28D9'
  gradient-start: '#6D28D9'
  gradient-end: '#EC4899'
  on-gradient: '#FFFFFF'
  indicator: '#EDE4FF'
  ring-track: '#F2ECF5'
  warning: '#B45309'
  warning-container: '#FFEDC7'
  exceeded: '#E11D48'
  exceeded-container: '#FFF1F3'
  pending-sync: '#B45309'
  pending-sync-container: '#FFF1D6'
  inbox: '#4F46E5'
  inbox-container: '#EEF2FF'
  category-food: '#F59E0B'
  category-food-tint: '#FFE9C7'
  category-transport: '#10B981'
  category-transport-tint: '#D5F5E3'
  category-shopping: '#E11D48'
  category-shopping-tint: '#FFD6E0'
  category-bills: '#3B82F6'
  category-bills-tint: '#DDE7FF'
  category-fun: '#A855F7'
  category-fun-tint: '#F3E1FF'
  # Light: derived for the Orbit Rings structure from Jelly Pop values only (key-*.html)
  aurora-1: '#EDE4FF'
  aurora-2: '#FFD6E0'
  aurora-3: '#FFE9C7'
  glass: '#FFFFFFB8'
  glass-edge: '#FFFFFFE6'
  nav-glass: '#FFFFFFD9'
  scrim: '#2B183873'
  # Dark: Jelly Pop palette, copied exactly from direction-jelly-pop.html
  surface-canvas-dark: '#171021'
  surface-container-dark: '#241A33'
  on-surface-dark: '#F6EEFF'
  on-surface-variant-dark: '#B3A4C6'
  outline-variant-dark: '#33264A'
  edge-dark: '#0E0915'
  primary-dark: '#C4B5FD'
  gradient-start-dark: '#7C3AED'
  gradient-end-dark: '#DB2777'
  on-gradient-dark: '#FFFFFF'
  indicator-dark: '#3B2A5C'
  ring-track-dark: '#33264A'
  warning-dark: '#FBBF24'
  warning-container-dark: '#3D2C0A'
  exceeded-dark: '#FB7185'
  exceeded-container-dark: '#33141F'
  pending-sync-dark: '#FBBF24'
  pending-sync-container-dark: '#3D2C0A'
  inbox-dark: '#6366F1'
  inbox-container-dark: '#1F2148'
  category-food-dark: '#FBBF24'
  category-food-tint-dark: '#3A2A14'
  category-transport-dark: '#34D399'
  category-transport-tint-dark: '#11312A'
  category-shopping-dark: '#FB7185'
  category-shopping-tint-dark: '#3D1424'
  category-bills-dark: '#60A5FA'
  category-bills-tint-dark: '#172447'
  category-fun-dark: '#C084FC'
  category-fun-tint-dark: '#2E1A45'
  # Dark: derived
  aurora-1-dark: '#3B2A5C'
  aurora-2-dark: '#3D1424'
  aurora-3-dark: '#2E1A45'
  glass-dark: '#241A33B8'
  glass-edge-dark: '#33264A'
  nav-glass-dark: '#241A33E6'
  scrim-dark: '#0E0915A6'
typography:
  fontFamily:
    note: 'System sans (Roboto on Android) via M3 default FontFamily. No custom font in v1.'
  numerals:
    note: 'All amounts use tabular figures: fontFeatureSettings = "tnum".'
  display-amount:
    fontSize: 28sp
    fontWeight: 700
    letterSpacing: -0.02em
    note: 'Hero total and form amount field. Extends M3 Headline Medium. Android 14 non-linear scaling applies.'
  screen-title:
    fontSize: 22sp
    fontWeight: 700
    letterSpacing: -0.02em
    note: 'Tab titles (Expenses, Inbox, Budgets, Settings). Maps to M3 Title Large.'
  section-title:
    fontSize: 16sp
    fontWeight: 650
    note: 'Section heads (Categories). Maps to M3 Title Medium.'
  item-title:
    fontSize: 14sp
    fontWeight: 650
    note: 'Category and Expense names. Maps to M3 Title Small.'
  amount:
    fontSize: 15sp
    fontWeight: 700
    note: 'Row amounts. Maps to M3 Body Large at bold weight.'
  body:
    fontSize: 15sp
    fontWeight: 400
    lineHeight: 1.3
    note: 'Maps to M3 Body Large.'
  meta:
    fontSize: 11sp
    fontWeight: 400
    note: 'Secondary lines (time, "left of"). Maps to M3 Body Small.'
  overline:
    fontSize: 11sp
    fontWeight: 650
    letterSpacing: 0.05em
    note: 'Uppercase chart label only. Maps to M3 Label Small.'
  label:
    fontSize: 12sp
    fontWeight: 650
    note: 'Chips, segmented filter, nav label. Maps to M3 Label Medium.'
rounded:
  sm: 12px
  md: 18px
  lg: 24px
  xl: 28px
  full: 9999px
spacing:
  '1': 4px
  '2': 8px
  '3': 12px
  '4': 16px
  '5': 24px
  '6': 32px
  margin-mobile: 16px
  row-gap: 8px
  section-gap: 16px
components:
  glass-tile:
    background: '{colors.glass}'
    border: '1dp {colors.glass-edge}'
    blur: 18dp
    rounded: '{rounded.lg}'
    fallback: '{colors.surface-container}'
  aurora-background:
    layers: '{colors.aurora-1} top-left, {colors.aurora-2} top-right, {colors.aurora-3} mid'
    base: '{colors.surface-canvas}'
  hero-chart:
    container: '{components.glass-tile}'
    rounded: '{rounded.xl}'
    outer-ring-size: 112dp
    outer-ring-stroke: 12dp
    outer-ring-segments: 'category colors, 2 degree gaps, butt caps'
    inner-ring-stroke: 6dp
    inner-ring-fill: 'sweep {colors.gradient-start} to {colors.gradient-end}'
    track: '{colors.ring-track}'
  progress-ring:
    size: 48dp
    stroke: 6dp
    cap: round
    fill: 'category color'
    track: '{colors.ring-track}'
    warning-fill: '{colors.warning}'
    exceeded-fill: '{colors.exceeded}'
    over-lap-stroke: 4dp
    over-lap-color: '{colors.exceeded}'
  category-row:
    container: '{components.glass-tile}'
    rounded: '{rounded.lg}'
    min-height: 64dp
    warning-outline: '2dp {colors.warning} plus 20dp glow'
    exceeded-outline: '2dp {colors.exceeded} plus 20dp glow'
  expense-row:
    container: '{components.glass-tile}'
    rounded: '{rounded.lg}'
    category-orb: '40dp circle, tint background, category-color icon'
  inbox-item:
    container: '{components.glass-tile}'
    rounded: '{rounded.lg}'
    bank-orb: '40dp circle, {colors.inbox-container} background, {colors.inbox} icon'
  status-chip:
    rounded: '{rounded.full}'
    typography: '{typography.label}'
    neutral: '{colors.ring-track} / {colors.on-surface-variant}'
    warning: '{colors.warning-container} / {colors.warning}'
    exceeded: '{colors.exceeded-container} / {colors.exceeded}'
  sync-pill:
    rounded: '{rounded.full}'
    background: '{colors.pending-sync-container}'
    content: '{colors.pending-sync}'
  pill-nav:
    background: '{colors.nav-glass}'
    border: '1dp {colors.glass-edge}'
    blur: 20dp
    rounded: '{rounded.full}'
    item-height: 48dp
    active-item: 'gradient {colors.gradient-start} to {colors.gradient-end}, {colors.on-gradient} icon and label'
    inactive-item: '{colors.on-surface-variant} icon only'
    badge: '{colors.exceeded} with {colors.on-gradient} text'
  fab:
    size: 56dp
    shape: circle
    background: 'gradient {colors.gradient-start} to {colors.gradient-end}'
    content: '{colors.on-gradient}'
    halo: '6dp ring of {colors.glass} plus {colors.gradient-end} glow'
  period-filter:
    container: '{components.glass-tile}'
    rounded: '{rounded.full}'
    segment-height: 48dp
    selected: 'gradient {colors.gradient-start} to {colors.gradient-end}, {colors.on-gradient}'
    unselected: '{colors.on-surface-variant}'
  filter-chip:
    rounded: '{rounded.full}'
    height: 32dp
    unselected: '{colors.glass} with {colors.glass-edge} border and category dot'
    selected: 'gradient {colors.gradient-start} to {colors.gradient-end}, {colors.on-gradient}'
  button-primary:
    rounded: '{rounded.full}'
    height: 48dp
    background: 'gradient {colors.gradient-start} to {colors.gradient-end}'
    content: '{colors.on-gradient}'
  button-disabled:
    rounded: '{rounded.full}'
    background: '{colors.ring-track}'
    content: '{colors.on-surface-variant}'
  button-add:
    rounded: '{rounded.full}'
    height: 36dp
    touch-target: 48dp
    background: 'gradient {colors.gradient-start} to {colors.gradient-end}'
    content: '{colors.on-gradient}'
  text-button-destructive:
    content: '{colors.exceeded}'
    height: 40dp
    touch-target: 48dp
  text-field:
    rounded: '{rounded.md}'
    background: '{colors.glass}'
    border: '1dp {colors.outline-variant}'
    required-empty-border: '1.5dp dashed {colors.primary}'
  bottom-sheet:
    rounded: '{rounded.xl} top corners'
    background: '{colors.surface-container} with {colors.aurora-1} and {colors.aurora-2} glow at the top'
    scrim: '{colors.scrim}'
  dialog:
    rounded: '{rounded.xl}'
    background: '{colors.surface-container}'
    scrim: '{colors.scrim}'
    destructive-action: '{colors.exceeded} fill, {colors.surface-container} text'
  snackbar:
    rounded: '{rounded.full}'
    background: '{colors.on-surface}'
    content: '{colors.surface-canvas}'
  swipe-delete:
    background: '{colors.exceeded}'
    content: '{colors.surface-container}'
  swipe-dismiss:
    background: '{colors.on-surface-variant}'
    content: '{colors.surface-canvas}'
  color-swatch:
    size: 28dp
    touch-target: 48dp
    selected: 'check icon plus outer ring in the swatch color'
---

## Brand & Style

Expense Tracker turns a monthly budget into something you want to look after, the way a fitness ring makes a daily goal feel worth closing. Each Category is a progress ring on frosted glass, floating over a soft aurora glow. The mood is playful and premium: candy-tinted color and springy motion, held in check by calm medium-weight type and plenty of room around every number.

The structure comes from the Orbit Rings direction: frosted-glass tiles, an aurora background, progress rings that draw a red second lap when a Budget is exceeded, a floating pill navigation bar, and a round glowing FAB. Every shape is a circle or a pill. The color comes from the Jelly Pop direction: a grape-to-bubblegum gradient, a warm cream canvas in light mode, a grape-black canvas in dark mode, and one candy color per Category.

The product is private by design, and the visuals should feel personal rather than corporate or bank-like. The home footer says so plainly: "Private: lives in your own Google Sheet".

Built on Material 3 in Jetpack Compose. The tokens in this file's frontmatter map to M3 color roles where a role exists (noted in Colors) and extend the scheme where the brand needs more (gradient, glass, aurora, warning, pending sync, Category colors). Dynamic color (Material You) is off, because the brand palette is fixed.

Visual references: `mockups/direction-orbit-rings.html` (structure and shapes) and `mockups/direction-jelly-pop.html` (palette). Rendered key screens: `mockups/key-home.html`, `mockups/key-expenses.html`, `mockups/key-inbox.html`, `mockups/key-budgets.html`, `mockups/key-signin.html`, `mockups/key-settings.html`. **These spines (DESIGN.md and EXPERIENCE.md) win over the mocks wherever they disagree.**

## Colors

Hex values are copied exactly from `mockups/direction-jelly-pop.html`. The aurora, glass, nav-glass and scrim values are derived only from Jelly Pop values, as rendered in the key screens. Every light token has a `-dark` twin.

- **Canvas (`{colors.surface-canvas}` #FFF7F0 / #171021)**: the app background under the aurora. Maps to M3 `background` and `surface`. Warm cream in light mode, grape-black in dark mode.
- **Container (`{colors.surface-container}` #FFFFFF / #241A33)**: opaque surfaces such as sheets, dialogs, swipe-revealed rows, and the glass fallback. Maps to M3 `surfaceContainer`.
- **Ink (`{colors.on-surface}` #2B1838 / #F6EEFF)**: primary text and amounts. Plum, not black. Maps to M3 `onSurface`.
- **Muted (`{colors.on-surface-variant}` #7A6A86 / #B3A4C6)**: secondary lines, inactive nav icons, the Dismiss swipe background. Maps to M3 `onSurfaceVariant`.
- **Line (`{colors.outline-variant}` #F0E4DA / #33264A)**: field borders and the dark-mode glass edge. Maps to M3 `outlineVariant`.
- **Edge (`{colors.edge}` #EADCF0 / #0E0915)**: used only as the shadow tint and as the base of the dark-mode scrim.
- **Primary (`{colors.primary}` #6D28D9 / #C4B5FD)**: grape. Text links, the required-field prompt, the Cancel action, the selected-icon color on the indicator. Maps to M3 `primary`.
- **Gradient (`{colors.gradient-start}` → `{colors.gradient-end}`; #6D28D9 → #EC4899 light, #7C3AED → #DB2777 dark)**: the brand signature. Used only for the FAB, the active nav item, the selected period segment and filter chip, primary buttons, the Inbox Add button, the avatar, and the hero inner Budget ring. Never used for body surfaces or text.
- **On gradient (`{colors.on-gradient}` #FFFFFF)**: icons and labels on the gradient. Maps to M3 `onPrimary`.
- **Indicator (`{colors.indicator}` #EDE4FF / #3B2A5C)**: soft grape pill behind a selected or highlighted value. Maps to M3 `secondaryContainer`. Also the first aurora layer.
- **Ring track (`{colors.ring-track}` #F2ECF5 / #33264A)**: the unfilled part of every ring, neutral chips, disabled buttons, and the close button background. Maps to M3 `surfaceContainerHighest`.
- **Warning (`{colors.warning}` #B45309 / #FBBF24, container #FFEDC7 / #3D2C0A)**: a Category at 80% or more of its Budget. Amber ring, amber glow, amber chip. An M3 extension.
- **Exceeded (`{colors.exceeded}` #E11D48 / #FB7185, container #FFF1F3 / #33141F)**: a Category at 100% or more of its Budget, the second red lap, the over amount, destructive actions (Delete) and the nav badge. Maps to M3 `error` and `errorContainer`.
- **Pending Sync (`{colors.pending-sync}` #B45309 / #FBBF24, container #FFF1D6 / #3D2C0A)**: the header sync pill and the "Waiting to sync" row marker. It shares the amber hue with warning, but the pill and chip carry text and an icon, so they never rely on color alone.
- **Inbox (`{colors.inbox}` #4F46E5 / #6366F1, container #EEF2FF / #1F2148)**: the bank orb on Inbox Items. Indigo sets bank SMS apart from Categories.
- **Category colors**: Food #F59E0B, Transport #10B981, Shopping #E11D48, Bills #3B82F6, Fun #A855F7 in light; #FBBF24, #34D399, #FB7185, #60A5FA, #C084FC in dark. Each has a `-tint` for its icon orb background. The Category editor offers eight swatches: the five Category colors plus `{colors.gradient-start}`, `{colors.gradient-end}` and `{colors.inbox}` (`mockups/key-budgets.html`).
- **Aurora (`{colors.aurora-1}`, `{colors.aurora-2}`, `{colors.aurora-3}`)**: three soft radial glows behind content (indicator, bubblegum tint, food tint in light; indicator, shopping tint, fun tint in dark). Background decoration only.
- **Glass (`{colors.glass}` #FFFFFFB8 / #241A33B8, edge #FFFFFFE6 / #33264A, nav #FFFFFFD9 / #241A33E6)**: translucent tiles at about 72% opacity, the nav at about 85 to 90%.
- **Scrim (`{colors.scrim}` #2B183873 / #0E0915A6)**: behind sheets and dialogs.

Avoid: gradient fills on reading surfaces, Category colors used for state, and red anywhere except exceeded and destructive meanings.

**Contrast.** Every text and background pair meets WCAG AA: 4.5:1 for normal text, 3:1 for large text, in light, dark and the glass fallback. White `{colors.on-gradient}` on the gradient's pink end (#EC4899) is only about 3.5:1, so text that sits on the pink end either uses a darker pink (for example #DB2777, about 4.6:1 against white) or is large bold text. This rule covers the active nav label, the selected period segment and filter chip, primary buttons and the Add button. Some mocks were rendered before this rule and still show #EC4899 behind text. This rule wins over them.

**Dark mode.** Dark mode is a full second palette, not an inversion. Swap every token to its `-dark` twin. Glass becomes tinted container color (#241A33B8) instead of white, the glass edge becomes the line color, and the aurora uses deep grape, wine and plum. Warning, exceeded and Category colors lighten to keep contrast on the dark canvas. The app follows the system light or dark setting. There's no in-app theme toggle.

## Typography

The type is the system sans (Roboto) at medium weights: 650 for names and labels, 700 for amounts and titles, 400 for body and meta. These calm weights keep a playful palette feeling premium. Heavy 800 to 900 weights from Jelly Pop aren't used. Weight 650 renders as `FontWeight(650)` where the font supports variable weight, and as SemiBold (600) otherwise [ASSUMPTION].

Roles map to the M3 type scale (see each token's `note`). Sizes come from the mocks' 15px base and are scalable `sp`.

- Every amount uses tabular figures, so columns of numbers line up.
- `{typography.overline}` is the only uppercase style, used for the chart label ("October 2026 · All categories").
- Font scaling is honored up to at least 1.75x. Display sizes follow Android 14 non-linear scaling, so they grow less than body text. Layouts reflow instead of truncating amounts (see Layout & Spacing).
- Currency symbols and grouping follow the chosen Currency (INR by default, "₹14,500").

## Layout & Spacing

The scale is 4 / 8 / 12 / 16 / 24 / 32 dp. Screen side margins are `{spacing.margin-mobile}` (16dp). Rows sit `{spacing.row-gap}` (8dp) apart, and sections `{spacing.section-gap}` (16dp) apart. Content has bottom padding so the last row clears the floating nav and FAB.

Layout is a single column on phones. From top to bottom, each tab has a header (greeting or title, with the sync pill on the right), then filters, then content.

**Narrow and large text (320dp at 1.75x font), per the mocks:**
- The sync pill wraps under the greeting or title.
- The period filter wraps onto two lines inside a rounded-rectangle container (`{rounded.sm}`).
- The hero chart stacks above its legend.
- In a row, the amount wraps onto its own line under the name, and the chevron hides.
- Rings shrink and their strokes thin in proportion.
- The nav shows only the active item's label, and the FAB shrinks but keeps a 48dp touch target.

**Larger screens** (foldables and tablets, 600dp and wider): a navigation rail replaces the pill nav, and content is laid out in two columns within a readable maximum width.

## Elevation & Depth

Depth comes from light, not shadow. There are three layers:

1. The aurora background: three soft radial glows over the canvas. It's decoration only and never carries meaning.
2. Glass surfaces: translucent fill, a 1dp light edge, and an 18dp background blur (20dp for the nav). They have no M3 tonal elevation and no drop shadow.
3. Floating chrome and overlays: the pill nav, FAB, sheets, dialogs and snackbar. Each has one soft shadow tinted with ink or edge (never neutral grey), and the FAB adds a glass halo and gradient glow.

State glows: a warning or exceeded Category row has a 2dp outline in its state color plus a soft outer glow in that color. A glow is the only "lit" effect on a row.

**Glass fallback for minSdk 24.** Real background blur needs `RenderEffect` (API 31+). On API 24 to 30, glass surfaces render as an opaque `{colors.surface-container}` with the same 1dp edge and corner radius, over the same aurora. Text contrast must pass on both the blurred and the fallback surface. The aurora itself is a plain gradient and renders on every API level.

## Shapes

Every shape is round. Circles are used for rings, orbs, avatars, the FAB, color swatches and icon buttons. Pills (`{rounded.full}`) are used for the nav, nav items, chips, the sync pill, buttons, the search field, the period filter and the snackbar.

- `{rounded.lg}` (24dp): glass tiles and rows (Category, Expense, Inbox Item).
- `{rounded.xl}` (28dp): the hero chart, sheet top corners and dialogs.
- `{rounded.md}` (18dp): form fields.
- `{rounded.sm}` (12dp): the radius that pill-shaped elements fall back to at large font sizes, when they wrap to several lines.

There are no sharp corners and no square icon containers.

## Components

Specs below are visual only. Behavior lives in EXPERIENCE.md Component Patterns.

- **Hero chart** (`{components.hero-chart}`, `mockups/key-home.html`): a glass tile holding a segmented outer ring, where each segment is a Category color sized by its share of spending. Inside it, a thinner ring filled with the gradient shows the Budget used. The center shows the total spent in `{typography.display-amount}`. A legend lists each Category with a color dot and percentage. The footer shows "Inner ring: 74% of ₹19,500 budget" and an indicator pill with the Amount Left ("₹5,000 left"). For a custom range, the inner ring and the Amount Left pill are removed and the footer reads "Spending only. Budgets are monthly, so a custom range has no Amount Left."
- **Progress ring** (`{components.progress-ring}`): a round-capped arc in the Category color over `{colors.ring-track}`, with the Category icon in the center. At 80% or more, the arc turns `{colors.warning}`. At 100% or more, the arc closes in `{colors.exceeded}` and a thinner inner arc, the **second lap**, draws the overspend fraction in `{colors.exceeded}`. Above 200%, the second lap stays full and the label reads "₹X over". For a custom range, the ring shows the Category's share of spending and has no state colors. A Category with no Budget shows a ring of its share of spending, with no state colors, and a "No budget" label.
- **Category row** (`{components.category-row}`): ring, then name with a status chip under it, then an amount block on the right ("₹900 left of ₹6,000" with "₹5,100 spent"), then a chevron. A warning row gets the amber outline and glow and a chip like "85% · slow down". An exceeded row gets the red outline and glow, a chip reading "Exceeded", and the amount in `{colors.exceeded}` ("₹450 over of ₹4,000").
- **Status chip** (`{components.status-chip}`): a small pill. The neutral chip shows a percentage ("22%"), the warning chip is amber and the exceeded chip is red. It always carries words or numbers, never color alone.
- **Sync pill** (`{components.sync-pill}`): an amber pill in the header with a sync icon and a count ("1 waiting to sync"). The row-level version is a smaller chip reading "Waiting to sync".
- **Pill nav** (`{components.pill-nav}`): floats above the gesture bar with side margins. Five items. The active item widens to about twice the others, fills with the gradient, and shows icon plus label. Inactive items show the icon only. The Inbox nav item carries a red count badge. At 600dp and wider, a navigation rail replaces it, with the same five items, gradient active item and badge.
- **FAB** (`{components.fab}`): a round, gradient-filled button with a plus icon, a glass halo and a pink glow. It sits above the nav at the bottom right, on Home and Expenses only.
- **Period filter** (`{components.period-filter}`): a glass pill holding three segments: "This month", "Previous months" (with a dropdown icon) and "Custom range" (with a calendar icon). The selected segment fills with the gradient and shows the chosen range ("5 Oct – 20 Oct").
- **Search field**: a glass pill with a search icon and the placeholder "Search expenses".
- **Filter chip** (`{components.filter-chip}`): a glass pill with a Category color dot. "All" plus one chip per Category, in a horizontally scrolling row that fades at the right edge.
- **Expense row** (`{components.expense-row}`): a Category orb (tint background, colored icon), the description, a meta line ("Food · 1:42 PM"), and the amount on the right. Rows not yet synced add the "Waiting to sync" chip. Day headers ("Today", "Yesterday", "Tue, 6 Oct") show the day total on the right.
- **Inbox Item** (`{components.inbox-item}`): an indigo bank orb, the amount in a larger weight, the payee, and a meta line with the bank, masked account and time ("HDFC ••1234 · 1:42 PM"). It has a gradient **Add** button (`{components.button-add}`) and a "Show original SMS" / "Hide original SMS" text toggle in `{colors.primary}`. The expanded SMS shows in `{typography.meta}`.
- **Swipe backgrounds**: Delete on Expenses uses `{components.swipe-delete}` (red, with a trash icon and "Delete"). Dismiss on Inbox uses `{components.swipe-dismiss}` (muted, with "Dismiss"), so the two read differently.
- **Bottom sheet** (`{components.bottom-sheet}`): a drag handle, a header (title, source line, round close button), then fields. Used for the expense form and the Category editor.
- **Expense form fields** (`{components.text-field}`): Amount (large, `{typography.display-amount}`), Date and Time side by side, Description, and Category as a wrap of Category chips (orb plus name). An empty Category field, which is required, shows a dashed `{colors.primary}` border and a prompt line.
- **Category editor**: Monthly budget (with a ₹ prefix and the supporting text "Applies from October onward"), Name, a Color row of eight round swatches (`{components.color-swatch}`), an Icon grid of round toggle buttons, Save (`{components.button-primary}`), and "Delete category" (`{components.text-button-destructive}`).
- **Dialog** (`{components.dialog}`): centered text, a round icon orb in the exceeded container for destructive dialogs, the title, a muted body, and pill actions at the bottom right. Cancel is a text action in primary; the destructive action is filled red. The delete-Category dialog adds a Category picker under the body ("Move its N expenses to:"), above the Delete action.
- **Snackbar** (`{components.snackbar}`): an inverse pill (ink background, canvas text) above the floating nav, with a Category-colored orb on the left ("₹450 added to Food"). After a Dismiss it reads "Dismissed" with an **Undo** text action.
- **Add category tile**: a row with a dashed muted border and a round plus orb.
- **Empty-state illustration**: simple line art in Jelly Pop tints, bundled with the app, above a short line of text and one action (see EXPERIENCE.md State Patterns).
- **Sign-in screen** (`mockups/key-signin.html`): the aurora background, a centered brand block, and an action area above the gesture bar. There's no nav.
  - **Brand mark**: the rings motif, drawn as a static, decorative version of the hero chart. Five Category-colored outer segments with 2° gaps, an inner gradient orbit, and a glowing gradient core with a glass halo. The app name "Expense Tracker" sits under it at 28sp, weight 700.
  - **Sign-in button**: follows Google's Sign in with Google branding guidelines, with the official button styling, the Google "G" logo, and the label "Continue with Google". Use Google's light theme in light mode and its dark theme in dark mode, in the pill shape. This is the one exception to `{components.button-primary}`, and it's required for public release. The mock draws a gradient pill with no G logo; this spine wins.
  - **Privacy line**: a lock icon and "Private: lives in your own Google Sheet" in `{typography.meta}`, under the button.
  - **Signing in**: the button gives way to a gradient progress ring and "Setting up your Expense Sheet…". The privacy line stays.
  - At 320dp and 1.75x font, the mark shrinks, the app name grows less than body text (non-linear scaling), and the button label wraps.
- **Settings screen** (`mockups/key-settings.html`): the "Settings" title in `{typography.screen-title}`, then two sections headed in `{typography.section-title}`, then Sign out.
  - **Preferences**: Currency (a read-only `{components.text-field}` with a dropdown icon, showing "₹ INR · Indian rupee") and Display name (a `{components.text-field}`). Each has a muted help line in `{typography.meta}` under it.
  - **Google account**: one `{components.glass-tile}` holding the account row (a gradient avatar with the initial, the email, and "Signed in with Google"), a divider, the Expense Sheet row (a round orb in `{colors.indicator}` with a `{colors.primary}` sheet icon, "Expense Sheet", "In your Google Drive"), and an **Open my Sheet** text action in `{colors.primary}`. In the sync-error and needs-consent states, a `{components.sync-pill}` with an icon and text appears inside the tile.
  - **Sign out**: a full-width neutral glass pill with an ink label and a muted icon. It's not red, because signing out is routine. Only its warning dialog uses the destructive style.
  - **Sign-out dialog**: a `{components.dialog}` with the exceeded-container icon orb, the title "Sign out?", a muted body, Cancel in primary, and a filled red **Sign out**.

## Do's and Don'ts

| Do | Don't |
|---|---|
| Keep every shape a circle or pill | Use square cards, sharp corners or square icon tiles |
| Reserve the gradient for the FAB, active nav item, selected period segment and filter chip, primary buttons, Add, the avatar and the hero inner Budget ring | Fill reading surfaces or body text with the gradient |
| Pair every warning or exceeded color with words or numbers ("85% · slow down", "Exceeded", "₹450 over") and the second lap | Signal warning or exceeded by color alone |
| Meet WCAG AA for every text pair; use a darker pink or large bold text on the gradient's pink end | Put small white text on #EC4899 |
| Use Google's official Sign in with Google button, with the G logo, on Sign-in | Restyle the Google sign-in button with the brand gradient |
| Use red only for exceeded, destructive actions (Delete, the Sign out confirm) and the nav badge | Use red for the Dismiss swipe (it's muted) or for decoration |
| Use medium weights (650 to 700) and tabular figures for amounts | Use 800 to 900 display weights or proportional figures |
| Fall back to opaque `{colors.surface-container}` glass below API 31 | Ship unblurred translucent glass that drops text contrast |
| Use every token's `-dark` twin in dark mode | Invert colors or reuse light glass (#FFFFFFB8) on the dark canvas |
| Keep the aurora as background decoration | Let the aurora carry meaning or sit over text without a glass layer |
| Let rows reflow (amount under name) at large font scales | Truncate amounts or Category names |
