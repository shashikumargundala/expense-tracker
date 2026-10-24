# Review: prd-expense-tracker.md (lenses: structure, prose)

**Reviewed:** `_bmad-output/initiative-expense-tracker/prd-expense-tracker/prd-expense-tracker.md` (3,188 words, measured)
**Date:** 2026-10-08
**Lenses:** structure, then prose (prose runs on top of the structure findings)
**Reader type:** humans. **Style guide:** Microsoft Writing Style Guide.

**Purpose read:** This document exists to help the app's owner and the downstream BMad steps (UX, architecture, spec, tickets) agree on what v1 of Expense Tracker must do, using one fixed vocabulary and traceable FR, UJ and SM IDs.

**Structure model:** Strategic/Context (Pyramid). The document is in good shape overall: the vision comes first, the Glossary comes before the requirements, the FRs follow one consistent pattern, and every UJ is realized by at least one FR. The findings below are polish: numbering, traceability gaps against what §0 promises, Glossary drift in the UJs, and a few duplications.

**Verdict:** Ready to finalize after light polish. No structural rework is needed. Content and meaning are left unchanged.

## Findings

| # | Pass | Original Text | Revised Text | Changes |
|---|------|---------------|--------------|---------|
| 1 | structure | §2.3 heading: `### 2.3 Key User Journeys` | `### 2.2 Key User Journeys` | §2.2 is missing, so the numbering skips from 2.1 to 2.3 and looks like a section was dropped. 0 words. |
| 2 | structure | §0: "Features are grouped in §4 with globally numbered requirements (FR-1 to FR-23) that reference user journeys (UJ-1 to UJ-4)." | QUESTION / CONDENSE: "Features are grouped in §4 with globally numbered requirements (FR-1 to FR-23). Where a requirement realizes a user journey (UJ-1 to UJ-4), it says so." | FR-4, FR-5, FR-6, FR-17, FR-21, FR-22 and FR-23 have no "Realizes" line, so the current sentence promises traceability the body doesn't deliver. Either reword §0 (shown) or add the links (see #3). +7 words. |
| 3 | structure | FR-6: "The User can edit an Expense's amount, Category, description and date in the app, and the change is written to the Expense Sheet." | "The User can edit an Expense's amount, Category, description and date in the app, and the change is written to the Expense Sheet. Realizes UJ-3." | The UJ-3 edge case ("an offline edit to the same row was waiting") and FR-9 both depend on in-app editing, but FR-6 has no link back to the journey. Adding it closes the only clear missing trace. +2 words. |
| 4 | structure | UJ-4 step 2: "Past 80% of the budget, Food is flagged as a warning, and past 100% as exceeded, showing how much over." | "At 80% or more of the budget, Food is flagged as a warning, and at 100% or more as exceeded, showing how much over." | "Past" means strictly above. FR-20 says "80% or more" and "100% or more". This aligns the journey with the normative FR so the boundary case isn't read two ways. +4 words. |
| 5 | structure | §4.4 "**Feature-specific NFR:** SMS content is processed only on the phone. Raw SMS text is never uploaded. Only the Expenses the User Adds go to the Expense Sheet." (~27 words) | MERGE into NFR-1: "**Feature-specific NFR:** see NFR-1 (raw SMS text never leaves the phone)." | Repeats NFR-1 almost word for word, and FR-10 and Non-Goals also restate it. Keeping one source of truth prevents the two from drifting apart. Saves ~15 words. |
| 6 | structure | §6.2 "Analyze my finances": "[NOTE FOR PM: this has to stay consistent with the privacy-first positioning. Decide in v2 planning whether the model runs on the phone or in the cloud, and what data leaves the device.]" (~33 words) | CONDENSE: "It must stay consistent with the privacy-first positioning; see Open Question 1." | Open Question 1 already asks this exact question and names an owner. A leftover "NOTE FOR PM" also reads as an unfinished draft in a finalized PRD. Saves ~21 words. |
| 7 | structure | §6.1 bullet 1: "All of §4: sign-in and Expense Sheet, Expenses, sheet sync and offline, SMS Inbox, Categories and Budgets, spending analytics, profile and settings (FR-1 to FR-23)." and bullet 2: "SMS detection for Indian banks only (HDFC, ICICI, SBI, Union Bank and similar), debits only, including UPI and card." | CONDENSE: "All of §4 (FR-1 to FR-23)." and "SMS detection for Indian banks only, debits only, including UPI and card (see FR-11)." | Bullet 1 repeats the §4 headings, and the bank list appears for the third time (UJ-1, FR-11, §6.1). Saves ~25 words. |
| 8 | structure | Line 9 placeholder: "*Working title. Confirm.*" | QUESTION: delete the line, or move it to §9 as "5. **Product name:** 'Expense Tracker' is a working title. (Owner: product. Before public release.)" | §10 says every assumption is confirmed, but this unresolved marker contradicts it. Open items belong in §9. ~0 words net. |
| 9 | structure | §4.6 and §4.7 have no **Description:** line, and §4.2 has a Description without a "Realizes" trace | PRESERVE / optional: add "**Description:** The User sees where the money went this month." (§4.6) and "**Description:** The User adjusts personal display settings." (§4.7) | Every other feature group opens with a Description. Optional for consistency only, since these sections are short enough to read without one. +~20 words. |
| 10 | prose | UJ-2: "the app creates a new spreadsheet in his Google Drive", "finds his existing spreadsheet", "creates a new one only if none exists", "if he deleted the spreadsheet from Drive"; UJ-2 Resolution, UJ-3 and the §4.3 heading use "the sheet"; FR-8/FR-9 titles: "Sheet edits…", "The sheet wins conflicts" | Use the Glossary term: "creates a new Expense Sheet in his Google Drive", "finds his existing Expense Sheet", "if he deleted the Expense Sheet from Drive", "the Expense Sheet is the record of truth"; titles "FR-8: Expense Sheet edits show up in the app", "FR-9: The Expense Sheet wins conflicts", "### 4.3 Expense Sheet sync and offline" | §0 says the Glossary fixes the vocabulary, yet the journeys use three names for one concept. Downstream skills read these as separate things. Keep "Google Sheets" only where it names the product. |
| 11 | prose | UJ-1: "When two SMS show the same transaction number, only one appears. When there is no number, or the numbers differ, both appear and he decides." Edge case: "a duplicate SMS with no transaction number" | "When two SMS show the same Transaction Reference, only one appears. When there is no Transaction Reference, or the references differ, both appear and he decides." Edge case: "a duplicate SMS with no Transaction Reference" | Aligns the journey with the Glossary term used in FR-12. |
| 12 | prose | UJ-1 step 6 / Climax / Resolution: "He taps **Add** on an item", "the item leaves Inbox", "added and dismissed SMS never reappear in Inbox" | "He taps **Add** on an Inbox Item", "the Inbox Item leaves Inbox", "Added and Dismissed Inbox Items never reappear in Inbox" | Uses the defined terms Inbox Item, Add and Dismiss consistently with FR-15 and FR-16. |
| 13 | prose | FR-9: "When the same Expense changed in the Expense Sheet while an edit to it was Pending Sync, the Expense Sheet's version is kept." | "When an Expense is changed in the Expense Sheet while an in-app edit to it is still Pending Sync, the Expense Sheet's version is kept." | Fixes the tense ("changed" → "is changed"), removes "the same" (which has no earlier referent), and makes clear which edit is pending. |
| 14 | prose | FR-15: "…and a \"₹450 added to Food\" confirmation appears." | "…and a confirmation such as \"₹450 added to Food\" appears." | In a requirement, a literal value reads as a fixed string. Marking it as an example keeps the requirement general. |
| 15 | prose | FR-10: "…explaining what is read and that it never leaves the phone except as Expenses the User Adds." | "…explaining what is read and that SMS content never leaves the phone, except as Expenses the User Adds." | "It" has no clear antecedent (the app? the permission? the SMS?). |
| 16 | prose | FR-2 bullet 1: "containing the Expense and Budget columns and the Source column." | "containing the columns for Expenses (including Source) and for Budgets." | Source is an Expense field (see Glossary), so listing it separately suggests a third kind of column. |
| 17 | prose | FR-16 title: "Dismiss is permanent" | Consider: "FR-16: Added and Dismissed Inbox Items never return"? | The body also covers Added items. The current title only names half of the rule. |

2 further minor fixes:
- In UJ-4 and FR-18/FR-19, lowercase "budget" appears beside capitalized "Budget". Capitalize it when it means the Glossary term (UJ-4 steps 2–3, Resolution, Edge case).
- The Glossary's "User" is also a Source value (FR-8: "filled in with Source = User"). This is readable as written. Consider formatting the values as code (`App`, `User`) in the Glossary, FR-3 and FR-8 to tell them apart.

## Summary

- **Recommendations:** 17 table rows plus 2 minor fixes (9 structure, 8 prose rows).
- **Estimated reduction if all are accepted:** about 60 words cut and about 33 words added, so a net reduction of about 30 words (~1% of 3,188). This document's problems are consistency and traceability, not length. No length target was provided.
- **Comprehension trade-offs:** none. No cut removes examples, journeys, or reinforcing summaries. The bank list stays in UJ-1 and FR-11, where it gives useful concrete detail.
- **Cross-reference check:**
  - Every UJ (1–4) is realized by at least one FR.
  - Every FR, UJ and SM ID cited in §0, §6.1 and §7 exists.
  - SM-1's validated FRs (FR-3, FR-7, FR-11, FR-15, FR-20) all exist.
  - SM-C1 correctly counterbalances SM-1.
  - `addendum.md` exists beside the PRD, as §0 says.
  - The only broken references are the §2.2 numbering gap (#1) and the overstated FR-to-UJ claim in §0 (#2/#3).
