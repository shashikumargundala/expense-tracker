# Google Cloud setup for sign-in

The app signs in with Google through Credential Manager. That needs one Google Cloud project with a consent screen, the Drive and Sheets APIs, a Web OAuth client and an Android OAuth client for each signing key. Client IDs aren't secrets. No client secret is ever downloaded into the app or committed.

You do this once. Steps 8 and 9, the release client and publishing, were added for the Play release (story 1.7).

## 1. Create the project

1. Open <https://console.cloud.google.com/> and sign in with the Google account that will own the app.
2. In the project picker at the top, click **New project**.
3. Name it `Expense Tracker`, leave the organization as it is, and click **Create**.
4. Make sure the new project is selected in the project picker for every step below.

## 2. Enable the Drive and Sheets APIs

1. Go to **APIs & Services > Library**.
2. Search for **Google Drive API**, open it and click **Enable**.
3. Go back to the library, search for **Google Sheets API**, open it and click **Enable**.

Sign-in itself doesn't call them. Stories 2.2 and 2.3 do, and enabling them now keeps the setup in one place.

## 3. Configure the consent screen (starts in Testing mode)

1. Go to **APIs & Services > OAuth consent screen** (in newer consoles: **Google Auth Platform > Branding**). If asked, click **Get started**.
2. **App information:** app name `Expense Tracker`, and your email as the user support email. Click **Next**.
3. **Audience:** choose **External**. Click **Next**.
4. **Contact information:** your email. Click **Next**, agree to the policy, and click **Create**.
5. Go to **Audience** and check that the publishing status is **Testing**. It stays there while you develop; step 9 publishes it to production for the Play release.
6. Under **Test users**, click **Add users** and add every Google account that will sign in, including your own. While the status is Testing, only these accounts can sign in while the app is in Testing. After step 9, any Google account can sign in.
7. Go to **Data access** and click **Add or remove scopes**. Find `.../auth/drive.file` (*See, edit, create and delete only the specific Google Drive files you use with this app*), tick it, click **Update**, then **Save**. Add no other Drive or Sheets scope.

## 4. Create the Web OAuth client

Credential Manager uses this client's ID as the server client ID. The app never uses its secret.

1. Go to **APIs & Services > Credentials** (or **Google Auth Platform > Clients**).
2. Click **Create credentials > OAuth client ID** (or **Create client**).
3. **Application type:** **Web application**. **Name:** `Expense Tracker Web`.
4. Leave the JavaScript origins and redirect URIs empty. Click **Create**.
5. Copy the **Client ID**. It ends in `.apps.googleusercontent.com`. Ignore the client secret, and don't download the JSON.

## 5. Create the debug Android OAuth client

This client lets debug builds signed with your debug key sign in.

1. In **Credentials**, click **Create credentials > OAuth client ID** again.
2. **Application type:** **Android**. **Name:** `Expense Tracker Android debug`.
3. **Package name:** `com.shashi.expensetracker`
4. **SHA-1 certificate fingerprint:** the debug key's SHA-1:

   ```text
   EA:0A:C8:A2:95:A7:87:DB:D5:A2:79:EF:A2:80:AA:52:21:69:D3:B5
   ```

   That's the SHA-1 of `~/.android/debug.keystore` on the owner's machine. To check it, or to get the SHA-1 on another machine, run:

   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1
   ```

   Each machine that builds debug APKs for sign-in needs its SHA-1 registered. Add it to this client, or create another Android client.
5. Click **Create**. There's nothing to copy; Google matches the package name and signing key.

## 6. Paste the Web client ID into the build

Open `gradle.properties` at the repo root and set:

```properties
googleWebClientId=123456789012-abc123def456.apps.googleusercontent.com
```

Use your Web client ID from step 4, not the Android client's. It's not a secret, so commit it. The `:data` module turns it into `BuildConfig.GOOGLE_WEB_CLIENT_ID` for each build type.

With the value blank (as in CI), the app still builds and runs. Tapping Continue with Google shows "Sign-in isn't set up in this build." instead of the account picker.

## 7. Check it on the phone

1. Install the debug build: `./gradlew :app:installDebug`.
2. Open the app. Sign-in shows, with no navigation.
3. Tap **Continue with Google** and pick a test user's account. Home opens.
4. Close the app fully and open it again. Home opens directly, offline too.
5. To try cancelling, clear the app's data, open it, tap **Continue with Google** and dismiss the picker. You stay on Sign-in with no message.

If sign-in fails with "Couldn't sign in", check that the account is a test user (while the consent screen is still in Testing), that `googleWebClientId` is the Web client's ID, and that the debug client's package name and SHA-1 match the build.

## 8. Create the release Android OAuth client

Play-installed and shared APK builds are signed with the release key, so they need their own Android client.

1. In **Credentials**, click **Create credentials > OAuth client ID** (or **Google Auth Platform → Clients → Create client**).
2. **Application type:** **Android**. **Name:** `Expense Tracker Android release`.
3. **Package name:** `com.shashi.expensetracker`
4. **SHA-1 certificate fingerprint:** `FB:80:94:AD:5B:E5:AF:26:C4:9D:EF:17:5F:73:61:6C:10:78:FB:25`, the release key from story 1.9, which Play App Signing reuses.
5. Click **Create**. There's nothing to copy.

## 9. Publish the consent screen to production

**Google Auth Platform → Audience → Publish app**, then confirm.

- The app only asks for `drive.file`, a non-sensitive scope, so Google doesn't require verification and nothing costs money.
- After publishing, **any** Google account can sign in, including Play's reviewers and testers you haven't listed.
- The test-user list no longer limits sign-in.

