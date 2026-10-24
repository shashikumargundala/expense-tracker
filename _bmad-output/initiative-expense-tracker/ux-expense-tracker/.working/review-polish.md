---
title: Polish review (bmad-review, lenses structure + prose)
target: DESIGN.md, EXPERIENCE.md
date: 2026-10-08
---

# Polish review

Content class: docs. Lenses: structure, then prose (run on top of structure's findings). Style guide: Microsoft Writing Style Guide. Reader: humans (plus AI build agents).

**Purpose read:** the two spines give builders and AI build agents one agreed spec: DESIGN.md for how the app looks, EXPERIENCE.md for how it behaves, each pointing to the other rather than repeating it.

**Structure model:** Reference/Database (Key Flows: Tutorial/Linear). DESIGN.md section order, tokens and hex values treated as fixed.

## Structure

| # | Location | Disposition | Note | Applied |
|---|---|---|---|---|
| S1 | EXP Interaction Primitives | CONDENSE bullets that repeat Component Patterns (period filter, Load more, Home Category, Inbox hint) | Period-filter rule repeated verbatim (~110 words) | Partly: period filter, Load more, Inbox hint condensed to pointers; tap/swipe kept (unique details) |
| S2 | EXP State Patterns Warning/Exceeded/Far over/No Budget/Custom range | CONDENSE visual detail, point to DESIGN | ~70 words | Declined |
| S3 | DESIGN Components behavior clauses | QUESTION/CONDENSE triggers | ~60 words | Declined |
| S4 | EXP delete dialog copy in 3 places | MERGE into Component Patterns Delete dialog | ~45 words | Yes |
| S5 | EXP "₹450 added to Food" in 3 rows | MERGE into Snackbar row | ~30 words | Yes |
| S6 | EXP Accessibility Floor reflow list | CONDENSE, point to DESIGN Layout & Spacing | ~40 words | Yes |
| S7 | 600dp rail stated 4 times | CONDENSE to 2 homes | ~25 words | Declined |
| S8 | EXP Accessibility Floor Contrast bullet | CONDENSE, point to DESIGN Colors | ~25 words | Yes |
| S9 | EXP Settings contents item 3 | CONDENSE, point to State Patterns | ~25 words | Yes |
| S10 | EXP H3 Settings under Key Flows; Settings contents after flows | MOVE Settings contents after IA; fold stub | Schema break | Yes |
| S11 | EXP "English only in v1." in Voice and Tone and Language | MERGE | ~15 words | Yes (kept in Language); DESIGN Typography currency line kept |
| S12 | EXP IA "→ Composition:" line | CUT | ~10 words | Yes |
| S13 | Mock lists repeated in bodies | CONDENSE to "see sources" | ~55 words | Declined |
| S14 | EXP IA Sign-in row | CONDENSE, point to State Patterns | ~20 words | Yes |
| S15 | EXP Voice "Other copy" list | CONDENSE strings that have their own home | ~30 words | Yes |
| S16 | Component naming between files | QUESTION | word-neutral | Partly: added `{components.filter-chip}` to Category filter chips row |
| S17 | Key Flows schema (UJ-4 climax and Resolution) | QUESTION | word-neutral | Declined |
| S18 | UJ-2 Edge case last sentence | MOVE out of edge case | not an edge case | Yes (own line) |
| S19-21 | DESIGN Colors hex bullets; Do's and Don'ts; EXP Motion numbers | PRESERVE | | Kept |

Structure summary: 21 recommendations (18 actionable, 3 PRESERVE); ~640 words (~7.5%) if all accepted.

## Prose

| # | Location | Original | Revised | Applied |
|---|---|---|---|---|
| P1 | EXP UJ-3 steps 4, 6, edge case; State Patterns Sync error | "the sheet" | "the Expense Sheet" (bottom sheet ambiguity) | Yes |
| P2 | EXP IA bullet; UJ-1 steps 5, 8; Saved row | "item(s)" / "Inbox adds" | "Inbox Item(s)"; reword | Yes |
| P3 | DESIGN Pill nav | "The Inbox item" | "The Inbox nav item" | Yes |
| P4 | EXP IA Home row | "(month periods only)" scope unclear | "plus Amount Left and Budget for month periods" | Yes |
| P5 | EXP Expense form row | "From Inbox Add, Amount, Date…" | "When opened from Inbox Add, the form pre-fills…" | Yes |
| P6 | EXP Expense form row | Save disabled rule | Consider: also until Amount > 0? | Declined (decision question) |
| P7 | EXP UJ-4 step 6 | "raise Food" | "raise Food's Budget" | Yes |
| P8 | EXP Category filter chips | dangling "when arriving" | "when the User arrives from a Category row on Home" | Yes |
| P9 | EXP Expense row, Day header, Inbox Item | dangling modifiers, missing article | reworded | Yes |
| P10 | Both files | inner ring has 4 names | "inner Budget ring" everywhere | Yes |
| P11 | EXP Foundation glossary | User, Transaction Reference not listed | Consider adding | Declined (glossary is the PRD's, verbatim) |
| P12 | EXP UJ-2 step 3 | "first expense" | "first Expense" | Yes |
| P13 | EXP UJ-1 edge case | "a duplicate SMS … appears twice" | "an SMS … appears twice" | Yes |
| P14 | EXP UJ-1 step 6 | doubled "and", date format | reworded, "8 Oct 2026, 1:42 PM" | Yes |
| P15 | EXP UJ-1 step 7 | "Save enables" | "Save becomes enabled" | Yes |
| P16 | EXP UJ-1, UJ-2 | "first ever" | "first-ever" | Yes |
| P17 | EXP UJ-2 edge case | "If he deleted" | "If he has deleted" | Yes |
| P18 | EXP State Patterns intro | "signing in in" | "the signing-in state in" | Yes |
| P19 | EXP Sync error row | "The pill turns to" | "The pill reads" | Yes |
| P20 | EXP Needs consent Surface cell | names a moment | "Sync pill; Settings (Google account row)" | Yes |
| P21 | EXP Loading row | "local mirror" undefined | Consider defining | Declined (architecture term, AD-18) |
| P22 | EXP Swipe to Dismiss | "once it's gone" | "once the snackbar is gone" | Yes |
| P23 | EXP Long-press | "opens Add and Dismiss" | "opens a menu with Add and Dismiss" | Yes |
| P24 | EXP Coverage Check | "its own section above" | "Settings contents" | Yes |
| P25 | EXP IA intro | "the app is five tabs" | "the app has five tabs" | Yes |
| P26 | DESIGN Brand & Style | "The tokens above" | "The tokens in this file's frontmatter" | Yes |
| P27 | DESIGN Contrast | "darker pink there", "This covers" | drop "there", "This rule covers" | Yes |
| P28 | DESIGN Colors Edge, Line | ambiguous "dark scrim and shadow tint", "dark glass edge" | reworded | Yes |
| P29 | DESIGN Layout narrow list | "selected item's label" | "active item's label" | Yes |
| P30 | DESIGN Layout; Shapes | "rounder container", "pill rows" | `{rounded.sm}` container; "pill-shaped elements" | Yes |
| P31 | DESIGN Expense form fields | "An empty required Category" | "An empty Category field, which is required," | Yes |
| P32 | DESIGN Inbox Item | "a larger weight" | Consider: heavier weight or larger size? | Declined (meaning unclear) |
| P33 | DESIGN Status chip | non-parallel clauses | explicit subjects | Yes |
| P34 | DESIGN Typography | "This calm weight is what keeps" | "These calm weights keep" | Yes |
| P35 | DESIGN Do's and Don'ts | "selected filter"; list differs from Colors | match Colors gradient list | Yes |
| P36 | EXP Category editor | "8 swatches" | "eight swatches" | Yes |
| P37 | EXP Motion | "all of this is off" | "all of this motion is off" | Yes |
| P38 | Microcopy | "Google Sheet" vs "Expense Sheet" | Consider | Declined (microcopy is decided) |
| P39 | Microcopy | "budget" lowercase in shown string | Consider | Declined (microcopy is decided) |
| P40 | Both files | no serial comma | Consider | Declined (author's consistent choice) |

Eight further minor prose fixes were offered on request and not expanded.
