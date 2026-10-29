# Releasing to Google Play (closed testing)

This is the first Play release: the app on the **closed testing** track, signed by Play App Signing with **the same release key as the shared APKs** (story 1.9). Console labels may differ slightly from these steps. Follow the on-screen text when they do.

## 0. Before you start

- `docs/PRIVACY.md` is live: enable **GitHub Pages** (repo Settings → Pages → Branch `master`, folder `/docs`). Check that `https://shashikumargundala.github.io/expense-tracker/PRIVACY` opens.
- Google Cloud (see `docs/GOOGLE_CLOUD_SETUP.md`):
  - The **release Android OAuth client** exists, with SHA-1 `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`.
  - The consent screen is **published to In production**.

## 1. Create the upload key (once)

Play uses your existing release key to sign installs, and you upload with a separate **upload key**. Run this in your own terminal:

```bash
keytool -genkeypair -v \
  -keystore ~/.android-keys/expense-tracker-upload.jks \
  -alias upload -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Expense Tracker Upload, O=Expense Tracker"
chmod 600 ~/.android-keys/expense-tracker-upload.jks
```

Save its password in your password manager, and back up the file the same way as the release key. If the upload key is lost, Play can reset it, unlike the release key.

## 2. Create the app

Play Console → **Create app**: name "Expense Tracker", default language, **App**, **Free**, then accept the declarations.

The Create app screen doesn't ask for a package name. Play takes it from the first AAB you upload, and it can't be changed afterwards. Ours is `com.shashi.expensetracker` (the `applicationId` in `app/build.gradle.kts`).

## 3. Play App Signing with your existing key

Under **Protected with Play → Play Store distribution → Go to Play app signing** (this was **App integrity** before Google moved it), choose to **use your own existing app signing key**, not a Google-generated one. If that choice isn't offered on a brand-new app, check whether Play only shows it once the first AAB is uploaded.

1. Play shows a command to download and run **PEPK**, with your encryption key. Run it with the **release** key:
   - `--keystore ~/.android-keys/expense-tracker-release.jks`
   - `--alias expense-tracker`
2. Upload the encrypted ZIP that PEPK writes.
3. When asked for the upload key certificate, export it and upload `~/.android-keys/upload_cert.pem` (written outside the repo):

   ```bash
   keytool -export -rfc -keystore ~/.android-keys/expense-tracker-upload.jks -alias upload -file ~/.android-keys/upload_cert.pem
   ```

4. Afterwards, the Play app signing page shows the **app signing key SHA-1**. It must equal `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`. Tell the assistant what it shows, so it can be recorded.

## 4. App content (Policy → App content)

- **Privacy policy:** `https://shashikumargundala.github.io/expense-tracker/PRIVACY`
- **App access:**
  - "All or some functionality is restricted."
  - Instructions: "Tap Continue with Google on the first screen and sign in with any Google account. The app then opens its home screen."
- **Ads:** No.
- **Content rating:** fill in the questionnaire. Category Utility/Productivity, no objectionable content.
- **Target audience:** 18 and over.
- **Data safety:** declare what this build does, and update the form when later versions add more.
  - **Personal info:** email address, plus **user IDs** (the Google account ID), used for account management. Both stay on the device.
  - **Not yet:** financial info. Add it, as the user's expense amounts stored in their own Drive, in the release that ships expense syncing (epic 3). Add SMS in the SMS epic.
  - Data is encrypted in transit (HTTPS), not shared with third parties, and users can delete it.
  - Keep `docs/PRIVACY.md` and this form saying the same thing.
- **Financial features:** declare it as a personal budgeting or expense-tracking app with no banking, loans, payments or crypto.
- **Government apps, news, health and the rest:** No.

## 5. Store listing (Grow users → Store presence → Main store listing)

A short and full description, the app icon (512×512), a feature graphic (1024×500) and at least 2 phone screenshots. You can take the screenshots from your phone.

## 6. Closed testing release

1. **Test and release → Testing → Closed testing:** create a track, or use "Closed testing - Alpha".
2. **Testers:** create an email list with yourself and your testers, and save it.
3. **Countries:** pick where testers are, for example India.
4. **Create release:** upload the AAB.
   - Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` for every upload after the first.
   - Build it with `scripts/build-release-bundle.sh`. It asks for the **upload** keystore password and writes `dist/expense-tracker-<versionName>-<versionCode>.aab`.
5. Add release notes, save, then **Review release → Start rollout**. Google reviews closed releases, which can take from hours to a few days.
6. When it's approved, share the tester **opt-in link** shown on the track page. Testers accept, then install from Play.

## Later

- Each new release: bump `versionCode`, run `scripts/build-release-bundle.sh`, and create a new release on the track.
- Production on a personal account needs a closed test with **at least 12 testers for 14 days**, then an application for production access (as of 2026; check Play Console's current rule).
