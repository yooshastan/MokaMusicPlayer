#!/usr/bin/env bash
# Configure Moka's GitHub Actions repository secrets from a trusted workstation.
# Never paste private keystores/passwords into chat, commits, or issue comments.
set -euo pipefail
set +x
umask 077
export LC_ALL=C

REPO="yooshastan/MokaMusicPlayer"
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

for tool in gh keytool base64 grep sed tr; do
  command -v "$tool" >/dev/null 2>&1 || die "Missing $tool."
done
gh auth status --hostname github.com >/dev/null 2>&1 ||
  die "Please log in with: gh auth login"
printf 'Target repository: %s\n' "$REPO"
printf 'This helper uses an EXISTING persistent Moka release keystore.\n'
printf 'Keystore file path: '
IFS= read -r keystore
[[ -f "$keystore" && -r "$keystore" && ! -L "$keystore" ]] ||
  die "Keystore must be an existing, readable regular file (not a symlink)."
printf 'Existing signing key alias: '
IFS= read -r key_alias
[[ -n "$key_alias" ]] || die "Key alias cannot be blank."

IFS= read -r -s -p 'Keystore password: ' store_pass
printf '\n'
IFS= read -r -s -p 'Key password (press Enter if same as keystore password): ' key_pass
printf '\n'
[[ -n "$store_pass" ]] || die "Keystore password cannot be blank."
if [[ -z "$key_pass" ]]; then key_pass="$store_pass"; fi
trap 'unset store_pass key_pass MOKA_TMP_STORE_PASSWORD' EXIT

# The keytool option reads a temporary process environment variable. No
# password is placed on a command line or printed in the terminal.
export MOKA_TMP_STORE_PASSWORD="$store_pass"
if ! details="$(keytool -list -v -keystore "$keystore" -alias "$key_alias" \
  -storepass:env MOKA_TMP_STORE_PASSWORD 2>/dev/null)"; then
  die "Cannot read that alias from the keystore. Check the file, password and alias."
fi
unset MOKA_TMP_STORE_PASSWORD
printf '%s\n' "$details" | grep -Fq 'Entry type: PrivateKeyEntry' ||
  die "That alias is not a private-key entry; refusing to configure signing."

actual="$(printf '%s\n' "$details" | sed -n 's/.*SHA256: *//p' |
  head -n 1 | tr -d ':\r[:space:]' | tr '[:upper:]' '[:lower:]')"
[[ "$actual" =~ ^[[:xdigit:]]{64}$ ]] ||
  die "Could not read a valid certificate SHA-256 fingerprint."

printf 'Expected public certificate SHA-256 (64 hex characters): '
IFS= read -r expected
expected="$(printf '%s' "$expected" | tr -d ':\r[:space:]' | tr '[:upper:]' '[:lower:]')"
[[ "$expected" =~ ^[[:xdigit:]]{64}$ ]] ||
  die "Expected certificate fingerprint must contain exactly 64 hexadecimal characters."
[[ "$expected" == "$actual" ]] ||
  die "The selected keystore certificate does NOT match the expected fingerprint. No secrets uploaded."

printf 'Certificate fingerprint matches the selected keystore.\n'
printf 'Configure the 5 repository secrets for %s? [y/N] ' "$REPO"
IFS= read -r confirm
[[ "$confirm" == y || "$confirm" == Y ]] || die "Cancelled without uploading secrets."

base64 -w0 "$keystore" | gh secret set MOKA_SIGNING_STORE_B64 -R "$REPO"
printf '%s' "$store_pass" | gh secret set MOKA_SIGNING_STORE_PASSWORD -R "$REPO"
printf '%s' "$key_alias" | gh secret set MOKA_SIGNING_KEY_ALIAS -R "$REPO"
printf '%s' "$key_pass" | gh secret set MOKA_SIGNING_KEY_PASSWORD -R "$REPO"
printf '%s' "$actual" | gh secret set MOKA_SIGNING_CERT_SHA256 -R "$REPO"

printf '\nChecking GitHub repository secret NAMES (values are never fetched):\n'
gh secret list -R "$REPO"
printf '\nSuccess: secret upload commands completed. Verify that all 5 names above appear.\n'
printf 'Start a NEW signed workflow run on master; do not reuse an old failed commit.\n'