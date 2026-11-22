#!/usr/bin/env bash
# Safe local Moka signing: uses ONLY the existing user-controlled release key.
# Requires a greater versionCode and the same signer as the uploaded Beta 6.3 APK.
set -euo pipefail
set +x
umask 077
export LC_ALL=C

repo_root="$(cd -- "$(dirname -- "$0")/.." && pwd -P)"
cd "$repo_root"
fail() { printf 'REFUSED: %s\n' "$*" >&2; exit 1; }

# Public certificate from uploaded Moka 4.0.0-beta06.3, versionCode 41.
# The APK's origin on Uptodown must still be independently confirmed.
required_cert='b61f5999305f84d71b696bdd7ee7eb0e5fdff40d241d8a729f0a98de4ecef0b4'
baseline_code=41

[[ -f gradlew && -f app/build.gradle.kts ]] ||
  fail 'Run from your Moka source checkout.'
if [[ ! -f keystore.properties ]]; then
  for variable in MOKA_SIGNING_STORE_FILE MOKA_SIGNING_STORE_PASSWORD MOKA_SIGNING_KEY_ALIAS MOKA_SIGNING_KEY_PASSWORD; do
    [[ -n "$(printenv "$variable" 2>/dev/null || true)" ]] ||
      fail 'Configure your EXISTING release keystore in local keystore.properties or MOKA_SIGNING_* environment variables; do not create a replacement key.'
  done
fi

for executable in java awk grep sed tr sha256sum cp find sort tail head; do
  command -v "$executable" >/dev/null 2>&1 || fail "Missing $executable"
done
sdk="$(printenv ANDROID_HOME 2>/dev/null || printenv ANDROID_SDK_ROOT 2>/dev/null || true)"
if [[ -z "$sdk" && -f local.properties ]]; then
  sdk="$(sed -n 's/^sdk\.dir=//p' local.properties | head -n 1)"
fi
[[ -n "$sdk" && -d "$sdk/build-tools" ]] ||
  fail 'Android SDK build-tools missing; configure ANDROID_HOME.'
aapt="$(find "$sdk/build-tools" -mindepth 2 -maxdepth 2 -type f -name aapt | sort -V | tail -n 1)"
apksigner="$(find "$sdk/build-tools" -mindepth 2 -maxdepth 2 -type f -name apksigner | sort -V | tail -n 1)"
[[ -x "$aapt" && -x "$apksigner" ]] ||
  fail 'Android aapt and apksigner are required.'

printf 'Checking public certificate of the local Gradle release signer...\n'
report="$(bash ./gradlew :app:signingReport --console=plain --no-build-cache)" ||
  fail 'Gradle signingReport failed.'
release_cert="$(printf '%s\n' "$report" | awk '
  /^Variant: release$/ { in_release=1; next }
  /^Variant: / { in_release=0 }
  in_release && /^SHA-256:/ { print $2; exit }
' | tr -d ':\r[:space:]' | tr '[:upper:]' '[:lower:]')"
[[ "$release_cert" =~ ^[a-f0-9]{64}$ ]] ||
  fail 'Release variant is unsigned or certificate not available.'
[[ "$release_cert" == "$required_cert" ]] ||
  fail "Local signer $release_cert differs from Beta 6.3 signer $required_cert. Refusing incompatible update."

printf 'Signer matches Beta 6.3. Building locally...\n'
bash ./gradlew :app:testDebugUnitTest :app:assembleRelease --no-build-cache ||
  fail 'Tests or signed APK build failed.'
apk='app/build/outputs/apk/release/app-release.apk'
[[ -s "$apk" ]] || fail 'Signed release APK missing.'
package_line="$("$aapt" dump badging "$apk" | grep '^package:' | head -n 1)"
[[ "$package_line" == *"name='com.mokamusic.player'"* ]] ||
  fail 'Wrong package ID.'
code="$(printf '%s\n' "$package_line" | sed -n "s/.*versionCode='\([0-9][0-9]*\)'.*/\1/p")"
version="$(printf '%s\n' "$package_line" | sed -n "s/.*versionName='\([A-Za-z0-9._-]*\)'.*/\1/p")"
[[ "$code" =~ ^[0-9]+$ && "$version" =~ ^[A-Za-z0-9._-]+$ ]] ||
  fail 'Cannot parse package version safely.'
(( code > baseline_code )) ||
  fail "versionCode $code must exceed the Beta 6.3 baseline ($baseline_code)."

signature_report="$("$apksigner" verify --verbose --print-certs "$apk")" ||
  fail 'APK signature verification failed.'
cert_rows="$(printf '%s\n' "$signature_report" |
  sed -nE 's/.*Signer.*certificate SHA-256 digest:[[:space:]]*([0-9a-fA-F:]+).*/\1/p')"
[[ "$(printf '%s\n' "$cert_rows" | wc -l)" -eq 1 ]] ||
  fail 'Expected one APK signing certificate.'
actual="$(printf '%s' "$cert_rows" | tr -d ':' | tr '[:upper:]' '[:lower:]')"
[[ "$actual" == "$required_cert" ]] ||
  fail 'APK signature does not match uploaded Moka Beta 6.3.'
if "$aapt" dump badging "$apk" | grep -q '^application-debuggable'; then
  fail 'Debuggable APK must not be distributed to Uptodown.'
fi
dist="$repo_root/dist/uptodown"
mkdir -p "$dist"
name="Moka-Music-Player-$version-v$code-uptodown-signed.apk"
cp "$apk" "$dist/$name"
( cd "$dist" && sha256sum "$name" > "$name.sha256" )
printf '\nSIGNED LOCAL APK VERIFIED\nPackage: com.mokamusic.player\nVersion: %s (code %s)\nSigner SHA-256: %s\nAPK: %s\nChecksum: %s\n' \
  "$version" "$code" "$actual" "$dist/$name" "$dist/$name.sha256"
printf '\nBefore submitting as an update, verify the uploaded Beta 6.3 APK was downloaded from the current Uptodown listing.\n'
