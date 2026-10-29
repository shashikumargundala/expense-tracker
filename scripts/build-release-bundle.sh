#!/usr/bin/env bash
# Builds the signed, R8-minified Android App Bundle (AAB) for Google Play into dist/.
# Signs with the UPLOAD key; Play re-signs installs with the app signing key. See docs/PLAY_RELEASE.md.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${KEYSTORE_PATH:=$HOME/.android-keys/expense-tracker-upload.jks}"
: "${KEY_ALIAS:=upload}"
case "$KEYSTORE_PATH" in
  *expense-tracker-release.jks)
    echo "KEYSTORE_PATH points at the release (app signing) key. Play uploads use the upload key; unset KEYSTORE_PATH and KEY_ALIAS or point them at expense-tracker-upload.jks." >&2
    exit 1 ;;
esac
if ! command -v jarsigner >/dev/null 2>&1; then
  echo "jarsigner (part of the JDK) is not on PATH; it's needed to check the bundle's signature." >&2
  exit 1
fi
if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "No upload keystore at $KEYSTORE_PATH. Create it first (docs/PLAY_RELEASE.md)." >&2
  exit 1
fi

# Passwords come from the environment, or are asked for without echo. Never pass them as arguments.
if [ -z "${KEYSTORE_PASSWORD:-}" ]; then
  if [ ! -t 0 ]; then
    echo "KEYSTORE_PASSWORD is not set and there is no terminal to ask for it." >&2
    exit 1
  fi
  read -rsp "Upload keystore password: " KEYSTORE_PASSWORD
  echo
fi
if [ -z "$KEYSTORE_PASSWORD" ]; then
  echo "The keystore password is empty." >&2
  exit 1
fi
# A PKCS12 keystore (keytool's default) uses one password for the store and the key.
: "${KEY_PASSWORD:=$KEYSTORE_PASSWORD}"
export KEYSTORE_PATH KEY_ALIAS KEYSTORE_PASSWORD KEY_PASSWORD

AAB=app/build/outputs/bundle/release/app-release.aab
rm -f "$AAB" # never pick up a stale bundle from an earlier build
./gradlew bundleRelease --console=plain
if [ ! -f "$AAB" ]; then
  echo "No release bundle was produced; check the KEYSTORE_* variables." >&2
  exit 1
fi

CERT="$(jarsigner -verify -verbose -certs "$AAB" 2>/dev/null | grep -m1 -E 'X\.509, ' || true)"
if [ -z "$CERT" ]; then
  echo "The bundle is not signed; refusing to upload it." >&2
  exit 1
fi
if grep -q "CN=Android Debug" <<<"$CERT"; then
  echo "The bundle is signed with the debug key; refusing to upload it." >&2
  exit 1
fi

VERSION_NAME="$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
VERSION_CODE="$(sed -n 's/.*versionCode = \([0-9][0-9]*\).*/\1/p' app/build.gradle.kts | head -n 1)"
mkdir -p dist
OUT="dist/expense-tracker-$VERSION_NAME-$VERSION_CODE.aab"
cp "$AAB" "$OUT"

echo
echo "Built $OUT"
echo "Signed by: ${CERT#*X.509, }"
echo "Upload it in Play Console → Test and release → Testing → Closed testing → Create release."
