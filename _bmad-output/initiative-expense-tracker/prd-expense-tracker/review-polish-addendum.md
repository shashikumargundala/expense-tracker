# Review: addendum.md (polish pass, lenses: structure, prose)

**Target:** `_bmad-output/initiative-expense-tracker/prd-expense-tracker/addendum.md` (361 words; Technical notes 175, Gaps 88, Market context 72). The target was not edited.
**Read:** This document gives downstream BMad steps (architecture, bmad-build) the technical constraints, code gaps, and market context that support the PRD but aren't requirements.
**Structure model:** Reference/Database (random-access bullets grouped by audience). The three-section split by consumer (architecture / bmad-build / context) fits the model well.
**Reader type:** humans. **Style guide:** Microsoft Writing Style Guide.

**Verdict:** Sound and compact. One duplicated gap, one split topic, a vague pointer, and a few terminology drifts from the PRD Glossary. No content problems.

## Findings

| Pass | Original Text | Revised Text | Changes |
| --- | --- | --- | --- |
| structure | §Gaps: "**Budgets aren't monthly.** ... Fixed by FR-18." and "**Category budgets are a single limit with no month.** `CategoryEntity.budgetLimit` needs per-month Budgets (FR-18, FR-19)." (~35 words) | MERGE into one bullet: "**Budgets aren't monthly.** `CategoryEntity.budgetLimit` is a single limit with no month, and `ExpenseViewModel.categorySpendMetrics` sums all-time spend, although the UI says "Monthly Budget Alert". Fixed by FR-18 and FR-19." | Both bullets describe the same gap from the entity and view-model sides and share FR-18. One source of truth for bmad-build (saves ~8 words). |
| structure | §Technical notes: "**Offline queue vs. sheet-as-truth.**" bullet (currently 3rd, after "Finding the sheet again") | MOVE directly after "**Data-model shift.**" | Both bullets cover the local store's role and conflict handling. Data-model shift ends with "Architecture must decide the local role (cache vs. offline queue)", and the offline-queue bullet answers part of that. Keeping them adjacent reads as one thread (0 words). |
| structure | §Technical notes: blank line between the first bullet (line 5) and the second (line 7); no blank lines elsewhere | CONDENSE spacing: remove the blank line at line 6 | The single blank line makes Markdown render the whole list as a loose list with uneven spacing. Spacing should be consistent with the other two sections (0 words). |
| structure | §Technical notes, "Finding the sheet again": "(see research in the memlog)" | "(see the research in `.memlog.md` beside this file)" | The memlog is a hidden dot-file. A reader of the addendum alone can't find it from "the memlog" (+3 words). |
| structure | §Market context (72 words) | PRESERVE | It may look out of scope for a technical addendum, but the PRD finalize step deliberately moved it here from the memlog. It gives positioning context that the PRD body doesn't carry. |
| prose | "The user decided (2026-10-08) that the user's Google Sheet is the store of record with no app server." | "On 2026-10-08 the User decided that the Expense Sheet is the record of truth, with no app server." | Removes the repeated "the user". Aligns with Glossary terms ("User", "Expense Sheet", "record of truth"), so the addendum doesn't introduce a second name for the same concept. |
| prose | "an app can see files it created itself, which also helps Google's OAuth verification" | "an app can see files it created itself. This narrow scope also eases Google's OAuth verification" | "which" has an unclear antecedent: it could mean the files or the scope. Splitting the sentence names the referent. |
| prose | "Architecture defines how changes are detected." | "Architecture must define how changes are detected." | Matches "Architecture must decide the local role" in the Data-model bullet. The present tense reads as if architecture already defines it. |
| prose | "...`oauth2callback` and holds a client secret." | "...`oauth2callback` and stores a client secret on the device." | "holds" is vague. "Stores ... on the device" states the actual concern. Consider: drop "on the device" if the location isn't the point. |
| prose | "Found while reading the code on 2026-10-07/08." | "Found while reading the code on 2026-10-07 and 2026-10-08." | The slash date range isn't a standard form. Spell out both dates. |
| prose | "**Category budgets are a single limit with no month.**" | (absorbed by the structure MERGE above) | If the MERGE is rejected, consider "Category Budgets" to match the Glossary capitalization used in the same bullet ("per-month Budgets"). |

## Summary

- 11 rows: 5 structure (1 MERGE, 1 MOVE, 1 CONDENSE, 1 pointer fix, 1 PRESERVE) and 6 prose.
- Estimated net change if all are accepted: about -5 words (about 1% of 361). No length target was given.
- Comprehension trade-offs: none. No cut removes examples, context, or orientation.

```json
[
  {"lens":"structure","location":"§Gaps, bullets 1 and 5","trigger_condition":"Two bullets describe the same non-monthly-budget gap (FR-18)","guard_snippet":"MERGE into: **Budgets aren't monthly.** `CategoryEntity.budgetLimit` is a single limit with no month, and `ExpenseViewModel.categorySpendMetrics` sums all-time spend, although the UI says \"Monthly Budget Alert\". Fixed by FR-18 and FR-19.","potential_consequence":"bmad-build may treat one gap as two fixes"},
  {"lens":"structure","location":"§Technical notes, bullet 3","trigger_condition":"Offline-queue bullet is split from the Data-model shift bullet it continues","guard_snippet":"MOVE directly after Data-model shift","potential_consequence":"Architecture reads the local-store question in two places"},
  {"lens":"structure","location":"§Technical notes, line 6","trigger_condition":"Lone blank line makes the list render loose","guard_snippet":"Remove blank line","potential_consequence":"Uneven rendering"},
  {"lens":"structure","location":"§Technical notes, Finding the sheet again","trigger_condition":"\"the memlog\" doesn't name the hidden file","guard_snippet":"(see the research in `.memlog.md` beside this file)","potential_consequence":"Reader can't locate the cited research"},
  {"lens":"structure","location":"§Market context","trigger_condition":"Looks out of scope but was deliberately moved here","guard_snippet":"PRESERVE","potential_consequence":"n/a"},
  {"lens":"prose","location":"§Technical notes, Data-model shift","trigger_condition":"Repeated \"the user\"; terms drift from Glossary","guard_snippet":"On 2026-10-08 the User decided that the Expense Sheet is the record of truth, with no app server.","potential_consequence":"Second name for a Glossary concept"},
  {"lens":"prose","location":"§Technical notes, Finding the sheet again","trigger_condition":"Ambiguous \"which\"","guard_snippet":"an app can see files it created itself. This narrow scope also eases Google's OAuth verification","potential_consequence":"Unclear what helps verification"},
  {"lens":"prose","location":"§Technical notes, Offline queue","trigger_condition":"Present tense implies the decision is done","guard_snippet":"Architecture must define how changes are detected.","potential_consequence":"Open decision may be missed"},
  {"lens":"prose","location":"§Technical notes, OAuth redirect","trigger_condition":"\"holds a client secret\" is vague","guard_snippet":"stores a client secret on the device","potential_consequence":"Concern understated"},
  {"lens":"prose","location":"§Gaps, intro","trigger_condition":"Nonstandard date range 2026-10-07/08","guard_snippet":"on 2026-10-07 and 2026-10-08","potential_consequence":"Minor ambiguity"},
  {"lens":"prose","location":"§Gaps, bullet 5","trigger_condition":"Capitalization inconsistent with Glossary (if MERGE rejected)","guard_snippet":"Category Budgets","potential_consequence":"Inconsistent terminology"}
]
```
