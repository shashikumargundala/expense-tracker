---
title: 'First release on the Play closed track'
type: 'chore'
ticket: '7'
created: '2026-10-28'
status: 'in-progress'
baseline_revision: 'ceeafcf357bd8f20b99c3d3a808bd09be6e3c3be'
route: 'oneshot'
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

**Problem:** The owner's Play developer account is now active, but the app isn't on Play. B11 asks for an R8 release on the closed test track and the Play App Signing SHA-1 recorded for the release OAuth client.

**Approach:**
1. **`scripts/build-release-bundle.sh`** builds the signed Android App Bundle (AAB) that Play requires, the same way `build-release-apk.sh` builds the APK. It uses the **upload** key's `KEYSTORE_*` variables and writes `dist/expense-tracker-<versionName>-<versionCode>.aab`.
2. **Play App Signing reuses story 1.9's key** as the app signing key, exported with Google's PEPK tool. A new upload key (`~/.android-keys/expense-tracker-upload.jks`) is created for uploads. This keeps shared APKs and Play installs compatible, and the release SHA-1 stays `FB:80:…:FB:25` (1.7's ticket note).
3. **`docs/PLAY_RELEASE.md`** is the owner's step-by-step guide:
   - create the app;
   - enrol in Play App Signing with the existing key;
   - App content: privacy policy, app access instructions for reviewers, ads (none), content rating, target audience, Data safety and Financial features;
   - create the closed testing track with a tester list, upload the AAB and roll out;
   - add testers and the install link;
   - bump `versionCode` for every upload.
4. **`docs/PRIVACY.md`**, the privacy policy. It covers what is collected (Google account email for sign-in), that data lives only on the phone and in the user's own Google Drive, that there are no servers, analytics or ads, the `drive.file` scope, and how to delete data.
5. **Owner hitl:** the owner does the Play Console steps, and the main session verifies a tester install from the closed track, which shows the app.
6. **Recording:** the Play App Signing SHA-1, as Play Console shows it, goes into `docs/RELEASE.md` and the epic notes (through bmad-ticket with the owner's OK).

**Decisions (owner, 2026-10-28):**
1. **Release OAuth client:** the release Android OAuth client (1.9 SHA-1) is registered now, in 1.7, so closed-track installs can sign in. Story 2.7 keeps its release checks.
2. **Consent screen:** the OAuth consent screen is published to **In production**. `drive.file` is non-sensitive, so no verification is needed, and any Google account, Play reviewers included, can sign in.
3. **Privacy policy:** hosted with GitHub Pages from `docs/`, at `https://shashikumargundala.github.io/expense-tracker/PRIVACY`. The owner enables Pages after this story's PR merges.

</frozen-after-approval>

## Implementation Notes

- Oneshot route: about 80 lines across one script and two docs, plus small doc pointers. Most of the story is the owner's Play Console work, guided by `docs/PLAY_RELEASE.md`.
- `scripts/build-release-bundle.sh` defaults to the upload key (`~/.android-keys/expense-tracker-upload.jks`, alias `upload`). It deletes any stale AAB, refuses unsigned or debug-signed bundles (checked through `jarsigner -verify -certs`), and reads the version from `app/build.gradle.kts`, because aapt2 can't read an AAB.
- Tested:
  - With no keystore it exits 1 with a clear message.
  - With a throwaway keystore it builds a 3.9 MB AAB signed `CN=Throwaway Upload` into `dist/`. The test AAB and keystore were deleted afterwards.
- `docs/GOOGLE_CLOUD_SETUP.md` gains step 8 (release Android client with 1.9's SHA-1) and step 9 (publish the consent screen to In production), per Decisions 1 and 2.
- Follow-up ticket edit (owner's decision 1): story 2.7's description still says the owner registers the release client. That step moved here, so 2.7 keeps only the release-APK checks. The edit goes through bmad-ticket after this build.
- `docs/PRIVACY.md` describes only today's behaviour (email, account ID, expenses, own Drive, `drive.file`, no servers, analytics or ads). It must be updated before the SMS epic ships.

## Verification

**Commands:**
- With no variables and no TTY, `scripts/build-release-bundle.sh` fails clearly. With the upload key, it produces a signed AAB in `dist/`. `jarsigner -verify -verbose:summary` on the AAB shows the upload certificate.
- `git status`: no `.jks` and nothing under `dist/`.

**Manual checks:**
- The closed-track listing installs on the owner's S23 through the tester link. The app opens and, depending on Q1, signs in.
- The Play App Signing SHA-1 in Play Console (App integrity) equals `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`.

## Review Triage Log

### Pass 1 (2026-10-28, quick)

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `PRIVACY.md` describes expense syncing and a Drive Sheet the current build doesn't have | medium | patch | Real: `:domain` has only `account` and `greeting`, and nothing calls `SheetsService` or `DriveService`. Those lines are now marked "arriving in upcoming versions", and the delete step is conditional. Over-disclosure is labelled, not removed. |
| 2 | `PLAY_RELEASE.md` App access tells reviewers the app creates a spreadsheet | medium | patch | Real (same evidence). It now says sign-in opens the home screen. |
| 3 | The Data safety guidance declares financial info and omits the account ID | medium | patch | Real. It now declares email plus user IDs, with financial info added in epic 3's release and SMS in the SMS epic. |
| 4 | The privacy policy names no developer or contact | medium | ask | Play expects a privacy contact in the policy. Asked the owner for a name and email. |
| 5 | `GOOGLE_CLOUD_SETUP.md` steps 3 and 7 say "stay in Testing" and "only test users", contradicting new step 9 | low | patch | Reworded so Testing applies while developing, until step 9. |
| 6 | The CLAUDE.md and Copilot "Web client ID" bullet doesn't mention the release client or production; "until the Play release" is ambiguous | low | defer | Agent-context files. Deferred. |
| 7 | Plan item 5 and the manual checks aren't met yet | false | reject | These are owner hitl steps, done after merge and before the story is closed. |
| 8 | The AAB script would accept a bundle signed with the release key when `KEYSTORE_*` is still exported from the APK flow | medium | patch | It now refuses a `KEYSTORE_PATH` ending in `expense-tracker-release.jks`. Tested: exits 1 with guidance. |
| 9 | A missing `jarsigner` shows up as "not signed" | low | patch | It now checks `command -v jarsigner` up front. |
| 10 | `upload_cert.pem` is written into the repo directory | low | patch | Now written to `~/.android-keys/`, and `*.pem` is gitignored. |
| 11 | Step 8 has no client name or Create click | low | patch | Now matches step 5's format. |

