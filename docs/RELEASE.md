# Building and sharing a release APK

Until the app is on Google Play, it's shared as a signed APK file. Every APK must be signed with the same release key. Android only installs an update over an older version when the keys match.

## 1. Create the release key (once)

Run this at your own terminal. Don't run it through an assistant, so the passwords never leave your machine:

```bash
mkdir -p ~/.android-keys && chmod 700 ~/.android-keys
keytool -genkeypair -v \
  -keystore ~/.android-keys/expense-tracker-release.jks \
  -alias expense-tracker -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Expense Tracker, O=Expense Tracker"
```

`keytool` asks for a password, using one password for both the keystore and the key. Choose a strong one.

The keystore never goes in this repo. `.gitignore` blocks `*.jks` and `*.keystore` as a safety net.

## 2. Back it up now

If you lose the keystore or its password, nobody can install updates over the APK you shared. They'd have to uninstall and reinstall. Their data is safe in their Google Sheet, but it's still painful.

- Put the password in your password manager.
- Copy `~/.android-keys/expense-tracker-release.jks` to two places that aren't this laptop, for example an encrypted cloud drive and a USB stick.
- Never email it or commit it.

## 3. Build

1. For every build you share, raise `versionCode` (and usually `versionName`) in `app/build.gradle.kts`. Android refuses an update whose `versionCode` isn't higher than the installed one.
2. Run:

   ```bash
   scripts/build-release-apk.sh
   ```

   It asks for the keystore password. You can also set `KEYSTORE_PASSWORD` in the environment, and `KEYSTORE_PATH` and `KEY_ALIAS` if you moved the key.

It builds the R8-minified release and refuses to continue if the result is unsigned or debug-signed. It writes `dist/expense-tracker-<versionName>-<versionCode>.apk`, which git ignores, and prints:
- the APK's SHA-256, which you can send alongside the file so recipients can check it;
- the signing certificate's SHA-1.

## 4. Share

- Send the APK file directly: chat, email or a shared drive.
- Recipients open it on their phone and allow **Install unknown apps** for the app they opened it from, such as Files or Chrome.
- Play Protect may warn that the app is from an unknown developer. They can choose **Install anyway**. The warning is more likely once the app reads SMS, which arrives in the SMS epic.
- To update, send the newer APK and they install it over the old one.

## Release key fingerprint

The release key's SHA-1 goes in the release Google OAuth client, which epic-account-and-sheet sets up for Google sign-in:

- SHA-1: `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`
- SHA-256: `25:41:E6:EE:9C:3B:58:93:6D:BE:26:25:40:A9:BF:6A:05:EE:D5:BF:D2:3E:54:96:48:D8:AE:92:8F:5C:69:C9`
- Certificate: `CN=Expense Tracker, O=Expense Tracker`, created 2026-10-27

`scripts/build-release-apk.sh` prints the same SHA-1 in lowercase without colons (`fb8094ad…fb25`), which is how `apksigner` writes it. Google Cloud accepts either form.

## Google Play (story 1.7)

The Play release follows `docs/PLAY_RELEASE.md`. Play App Signing **uses this existing key** (exported with PEPK), not a Google-generated one. That way, people who installed a shared APK can update from Play, and Google sign-in needs only this one release SHA-1. Play uploads are signed with a separate upload key, `~/.android-keys/expense-tracker-upload.jks`, through `scripts/build-release-bundle.sh`.

Play Console's app signing key SHA-1 was checked on 2026-10-29 and matches the release key: `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`.
