---
title: 'Sign-in on the signed release APK'
type: 'chore'
ticket: '7'
created: '2026-11-13'
status: 'built'
baseline_revision: '4e6bbb89c9e8977b10149634522305b1c7a4fcb0'
route: 'oneshot'
route_source: 'auto'
risk: 'medium'
review: 'none'
review_source: 'auto'
lenses_ran: []
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Epic 2's sign-in, Sheet find-or-create, offline cold start and Settings tile have only run on debug builds. The signed, R8-minified release APK that testers get might break them: a missing keep rule, or a release OAuth client that doesn't match the release key.

**Approach:**
- Build the release APK with `scripts/build-release-apk.sh`. The owner runs it, because it asks for the keystore password.
- Install it on the S23 in place of the debug build. The signatures differ, so the debug build must be uninstalled first.
- Check each of epic 2's features on it.
- Add R8 keep rules to `app/proguard-rules.pro` only where the release build fails.

The release Android OAuth client already exists (owner, 2026-11-13; `docs/GOOGLE_CLOUD_SETUP.md` step 8, release SHA-1 `FB:80:94:…:FB:25`).

**Checks on the release APK (S23):**
1. Sign-in shows the designed screen and signs in with the owner's test account, with no "isn't set up" or developer error.
2. It reuses the existing Expense Sheet: Drive still has exactly one "Expense Tracker" Sheet.
3. With airplane mode on, a force-stopped app cold-starts to Home.
4. Settings shows the account tile, and Open my Sheet opens the Sheet in the Sheets app.

</frozen-after-approval>

## Implementation Notes

The oneshot route fits: the expected code change is none, or a few keep rules. Most of the work is the owner's release build and the device checks. The release client was registered in story 1.7, so the owner's Cloud step is already done.

- **R8:** an unsigned `assembleRelease` (R8 and lint-vital) passed on the 2.6 code, and the signed release ran every check, so no keep rule was needed and the code is unchanged.
- **Build:** the owner ran `scripts/build-release-apk.sh` in a normal terminal. Under Claude Code's `!` there is no TTY for the password prompt, and the script correctly refuses. It produced `dist/expense-tracker-1.0-1.apk`, which `apksigner` reports as signed by `CN=Expense Tracker` with SHA-1 `fb8094ad…1078fb25`, the release client's fingerprint.
- **Install:** the debug build was uninstalled first because the signatures differ, which cleared only the local session.
- **S23 results (2026-11-13), all passed:**
  1. Sign-in showed the designed screen and signed in with the owner's test account, with no OAuth or developer error in logcat.
  2. Drive still has exactly one "Expense Tracker" Sheet; the owner checked.
  3. In airplane mode, a force-stopped app cold-started to Home in 161 ms with no crash.
  4. The Settings tile showed, and Open my Sheet opened the Sheet in the Sheets app.
- **Logcat:** a blocked Firebase Installations call came from Google Pay (`com.google.android.apps.nbu.paisa.user`), not this app. The release runtime classpath has no Firebase.
- **Review:** skipped, because the diff has no code, only this plan.

## Verification

**Commands:**
- `./gradlew assembleRelease` without signing variables -- expected: R8 and lint-vital pass (unsigned APK)
- `scripts/build-release-apk.sh` (the owner runs it) -- expected: a signed APK in `dist/` with the release SHA-1

**Manual checks:**
- Checks 1–4 in the Intent, on the S23.
