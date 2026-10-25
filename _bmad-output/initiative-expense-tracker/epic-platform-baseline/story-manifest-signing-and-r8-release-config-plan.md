---
title: 'Manifest, signing and R8 release config'
type: 'chore'
ticket: '2'
created: '2026-10-25'
status: 'built'
baseline_revision: '7629a01036765c1ff1cf48f936617c04eec37c71'
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

**Problem:** Story 1.1 left release builds unminified and unsigned, kept `allowBackup="true"`, and has nothing stopping a later dependency from merging `GET_ACCOUNTS` back into the manifest. The spine says the phone database is never copied anywhere ("Your Sheet is the backup") and that releases are R8-minified and signed from environment variables.

**Approach:**
- **Manifest** (`app/src/main/AndroidManifest.xml`):
  - Set `android:allowBackup="false"`.
  - Point `android:dataExtractionRules` at a new `res/xml/data_extraction_rules.xml` that excludes every domain from both `cloud-backup` and `device-transfer`. On Android 12+, `allowBackup=false` alone still allows device-to-device transfer.
  - Declare `GET_ACCOUNTS` with `tools:node="remove"`, so the merged manifest never contains it.
- **Release build type** (`app/build.gradle.kts`):
  - Turn on `isMinifyEnabled` and `isShrinkResources`.
  - Sign from `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`, read through `providers.environmentVariable` so the configuration cache tracks them.
  - When any of the four is missing, leave the release unsigned and log a warning, so `./gradlew build` and CI still work without secrets. Never fall back to the debug key.
- **Keep rules** (`app/proguard-rules.pro`): keep `@com.squareup.moshi.JsonClass` model classes (constructors and fields) and keep generic `Signature` and annotation attributes for Retrofit services. Each rule gets a one-line comment. Moshi and Retrofit aren't dependencies yet; the rules start matching when epics 2–3 add models.
- **Docs**: update the signing and R8 lines in `CLAUDE.md`, `.github/copilot-instructions.md` and the README.
- **Out of scope:**
  - Credential Manager keep rules (epic account-and-sheet).
  - `READ_SMS` (epic sms-inbox).
  - Creating the real upload keystore (story 1.7).

</frozen-after-approval>

## Implementation Notes

Oneshot route: about 60 lines across the manifest, one new XML file, `app/build.gradle.kts`, `proguard-rules.pro` and three docs, all simple config.

- **Surprise:** `finance_app_logo_1779726000241.png` is really a JPEG (`file` reports JFIF, 1024×1024). Release builds crunch PNGs, and AAPT2 failed on it; debug builds don't crunch, which hid the problem in 1.1. I renamed it to `.jpg` (`git mv`). The resource name is unchanged, so `ic_launcher_foreground.xml` needs no edit.
- `KEYSTORE_PATH` resolves through `rootProject.file`, so it's absolute or relative to the repo root.
- The missing-variables warning prints only when Gradle configures the build. A configuration-cache hit doesn't reprint it, but changing an env var invalidates the cache.
- `.gitignore` now ignores `*.jks` and `*.keystore`, so an upload key can't be committed by accident.
- `proguard-rules.pro` also keeps `SourceFile`/`LineNumberTable` (with source names renamed), so release stack traces stay readable. This is beyond the listed keep rules; it's cheap and harmless.
- AGP 9 writes the R8 mapping as `mapping.prt` (plus `configuration.txt`), not `mapping.txt`.
- Verified:
  - `./gradlew assembleDebug test assembleRelease` passes with all 9 tests green and produces `app-release-unsigned.apk`.
  - A signed build using a throwaway `keytool` keystore and the four env vars produces `app-release.apk` (1.2 MB, against 12.8 MB for debug).
  - `apksigner` reports `CN=Throwaway Test`.
  - `aapt2` shows `allowBackup=false`, `dataExtractionRules` set, and only the `INTERNET` permission (plus AndroidX's own dynamic-receiver permission).
  - The on-device launch of the minified build is still pending; that's the user's manual check.

## Verification

**Commands:**
- `./gradlew assembleDebug test`: expected to pass, with all 9 tests green.
- `./gradlew assembleRelease` with no signing env vars: expected to pass, producing `app-release-unsigned.apk` plus a warning.
- `./gradlew assembleRelease` with the four env vars pointing at a throwaway keystore made with `keytool` in the temp directory: expected to produce a signed `app-release.apk` and `app/build/outputs/mapping/release/mapping.prt` (AGP 9's mapping format), which proves R8 ran.
- `aapt2 dump xmltree --file AndroidManifest.xml app-release.apk`: expected `allowBackup` false, `dataExtractionRules` set, and no `GET_ACCOUNTS`.
- `apksigner verify --print-certs app-release.apk`: expected to show the throwaway certificate, not the debug key.

**Manual checks:**
- Install the signed release APK on the user's device and confirm the greeting shows. There's no emulator here.

## Review Triage Log

### Pass 1 (2026-10-25, quick)

| # | Finding | Verdict | Route | Evidence / action |
|---|---------|---------|-------|-------------------|
| 1 | `data_extraction_rules.xml` leaves out the `device_*` domains | medium | patch | Real: the data-extraction format also defines `device_root`, `device_file`, `device_database` and `device_sharedpref`, and D2D transfer still runs. Added all four to both sections. |
| 2 | Verification names `mapping.txt`, but AGP 9 emits `mapping.prt` | low | patch | Real; corrected the Verification line. |
| 3 | The 1.1 walkthrough links the old `.png` path | low | patch | Real dead link at line 84; it now points to `.jpg`. |
| 4a | Moshi rule has a two-line comment, not one | false | reject | Cosmetic wording with no effect on behaviour. |
| 4b | `SourceFile`/`LineNumberTable` keep is unrecorded | low | patch | Now recorded in Implementation Notes. Behaviour is unchanged. |
| 5 | Missing-signing warning prints on every configuration, not only `assembleRelease`, and the docs say "`assembleRelease` … logs a warning" | low | defer | Real noise on local and CI builds. A Configuration-Cache-safe way to warn only for release builds isn't trivial, and the doc fix edits CLAUDE.md. |

