---
title: 'Signed release APK for sharing'
type: 'chore'
ticket: '9'
created: '2026-10-27'
status: 'built'
baseline_revision: '9ef831bf460a9ec569dff1fa9794f87e7ba5c120'
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

**Problem:** There's no way to give the app to anyone yet. Play is waiting on account verification (story 1.7), and release builds stay unsigned because no release key exists. Story 1.2's minified-release device check is also still open.

**Approach:**
1. **The owner creates the release key** by running one `keytool` command at their own prompt, so the passwords never appear in this session.
   - The keystore lives outside the repo, at `~/.android-keys/expense-tracker-release.jks`, with alias `expense-tracker`, RSA 4096, and 30 years' validity.
2. **`scripts/build-release-apk.sh`:**
   - It reads the four `KEYSTORE_*` variables from the environment, or prompts silently for missing passwords.
   - It runs `./gradlew assembleRelease` and refuses to continue if the APK comes out unsigned.
   - It copies the result to `dist/expense-tracker-<versionName>-<versionCode>.apk` (gitignored), then prints its SHA-256 and the signing certificate's SHA-1.
3. **Phone check:** uninstall the debug app (different key), install the release APK on the owner's S23, launch it, and confirm the shell shows and the crash log is empty. This also closes story 1.2's check.
4. **`docs/RELEASE.md`:**
   - creating the key once;
   - backing it up: the keystore file plus the passwords in a password manager, stored in two places, with a warning that losing it breaks updates;
   - building with the script;
   - bumping `versionCode` for every shared build;
   - sharing, including "install unknown apps" and the Play Protect warning recipients may see;
   - the recorded release SHA-1;
   - the note that story 1.7 must reuse this key for Play App Signing.
5. **Ignore rules:** `.gitignore` adds `dist/` (`*.jks` and `*.keystore` are already ignored).

</frozen-after-approval>

## Implementation Notes

- Oneshot route: about 120 lines across one script, one doc, `.gitignore`, and a short pointer in `CLAUDE.md` and the Copilot instructions. The steps for the owner are a single `keytool` command and the phone check.
- The owner created the key and ran the build script in their own terminal (2026-10-27), so no password passed through the session. I tightened the keystore to mode 600; the folder was already 700.
- Release key: `CN=Expense Tracker, O=Expense Tracker`, SHA-1 `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`.
- The script's failure paths were tested: with no keystore, and with no password and no terminal, it exits 1 with a clear message. It deletes any stale `app-release.apk` before building, because an earlier throwaway-signed APK from story 1.2 could otherwise be copied.
- Verified: `dist/expense-tracker-1.0-1.apk` (1.4 MB). `apksigner` shows the owner's certificate, not the debug key. On the S23 (Android 16), the debug app was uninstalled and the release APK installed: it launches in 0.54 s, shows the greeting, switches all five tabs, Back returns to Home, and the crash log is empty. `pkgFlags` has no DEBUGGABLE and no ALLOW_BACKUP. This closes story 1.2's minified-release device check.
- The SHA-1 goes in `docs/RELEASE.md` and this plan. Recording it in the epic's Notes is a ticket-file edit, done afterwards through `bmad-ticket` with the user's OK; the build never writes ticket files.

## Verification

Dependencies are cached, so builds take 10–60 seconds. Run Gradle in the foreground with a timeout.

**Commands:**
- `git check-ignore -v ~/.android-keys/expense-tracker-release.jks dist/x.apk`: the keystore sits outside the repo and `dist/` is ignored.
- `scripts/build-release-apk.sh`, with the variables set: expected to produce a signed APK in `dist/`.
- `apksigner verify --print-certs dist/*.apk`: expected to show `CN` from the owner's key, not the Android Debug key.
- With no variables set, the script without a TTY: expected to fail clearly and not produce an unsigned APK in `dist/`.
- `adb install` followed by `am start`, a screenshot and `logcat -b crash`: expected to show the shell, with no crash.
- `git status`: expected to show no `.jks` and nothing under `dist/`.

## Review Triage Log

### Pass 1 (2026-10-27, quick)

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | With no `ANDROID_HOME` and no `local.properties`, `sed` fails and `set -e` exits silently | medium | patch | Reproduced by the reviewer. Added `\|\| true`. Re-tested: it now prints "No Android build-tools found…" and exits 1. |
| 2 | The "No build-tools" message can't be reached, because `ls` fails under pipefail | medium | patch | Same fix inside the subshell. Re-tested as above. |
| 3 | `aapt2 … \| head -1` can get SIGPIPE under pipefail | low | patch | Dormant at today's output size. Now captures the full output and keeps the first line. Parsing was re-tested on the real APK (`1.0`, `1`). |
| 4 | The doc's SHA-1 format differs from the script's output | low | patch | Added a note that the script prints lowercase without colons. |
| 5 | An empty password gives a vague "check the variables" message | low | patch | An explicit "The keystore password is empty." check now runs before the build. |
| 6 | `.idea/deploymentTargetSelector.xml` is modified in the working tree | false | reject | It's outside the diff and kept out of every commit with an explicit exclude. |

