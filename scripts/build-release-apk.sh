#!/usr/bin/env bash
# Builds the signed, R8-minified release APK into dist/ and prints its fingerprints.
# Setup, backup and sharing steps: docs/RELEASE.md.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${KEYSTORE_PATH:=$HOME/.android-keys/expense-tracker-release.jks}"
: "${KEY_ALIAS:=expense-tracker}"
if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "No keystore at $KEYSTORE_PATH. Create it first (docs/RELEASE.md)." >&2
  exit 1
fi

# Passwords come from the environment, or are asked for without echo. Never pass them as arguments.
if [ -z "${KEYSTORE_PASSWORD:-}" ]; then
  if [ ! -t 0 ]; then
    echo "KEYSTORE_PASSWORD is not set and there is no terminal to ask for it." >&2
    exit 1
  fi
  read -rsp "Keystore password: " KEYSTORE_PASSWORD
  echo
fi
if [ -z "$KEYSTORE_PASSWORD" ]; then
  echo "The keystore password is empty." >&2
  exit 1
fi
# A PKCS12 keystore (keytool's default) uses one password for the store and the key.
: "${KEY_PASSWORD:=$KEYSTORE_PASSWORD}"
export KEYSTORE_PATH KEY_ALIAS KEYSTORE_PASSWORD KEY_PASSWORD

SDK="${ANDROID_HOME:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null || true)}"
BUILD_TOOLS="$( (ls -d "$SDK"/build-tools/* 2>/dev/null || true) | sort -V | tail -1)"
if [ -z "$BUILD_TOOLS" ]; then
  echo "No Android build-tools found. Set ANDROID_HOME or sdk.dir in local.properties." >&2
  exit 1
fi

APK=app/build/outputs/apk/release/app-release.apk
rm -f "$APK" # never pick up a stale APK from an earlier build
./gradlew assembleRelease --console=plain
if [ ! -f "$APK" ]; then
  echo "The release build is unsigned; check the KEYSTORE_* variables." >&2
  exit 1
fi

CERTS="$("$BUILD_TOOLS/apksigner" verify --print-certs "$APK")"
if grep -q "CN=Android Debug" <<<"$CERTS"; then
  echo "The APK is signed with the debug key; refusing to share it." >&2
  exit 1
fi

BADGING="$("$BUILD_TOOLS/aapt2" dump badging "$APK")"
BADGING="${BADGING%%$'\n'*}" # first line: package name and versions
VERSION_NAME="$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<<"$BADGING")"
VERSION_CODE="$(sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" <<<"$BADGING")"
mkdir -p dist
OUT="dist/expense-tracker-$VERSION_NAME-$VERSION_CODE.apk"
cp "$APK" "$OUT"

echo
echo "Built $OUT"
echo "APK SHA-256: $(shasum -a 256 "$OUT" | cut -d' ' -f1)"
grep -E "certificate (DN|SHA-1 digest)" <<<"$CERTS"
