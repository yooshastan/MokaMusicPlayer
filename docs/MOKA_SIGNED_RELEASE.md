# Moka persistent-key signed APK — Beta 6.1

The merged Beta 6.1 preview (versionCode 39) was verified with a GitHub Actions **debug** certificate. To produce a stable, upgrade-compatible release APK, use an existing trusted Moka signing key if one exists. **Never replace a signing identity without planning for existing installations.**

## Maintainer's one-time private setup

1. Locate the **existing** Moka signing keystore if you have one. Confirm its certificate fingerprint locally using `keytool -list -v -keystore moka-release.jks -alias YOUR_ALIAS`. If you have never used a persistent release key, generate and back up a new long-lived key privately: `keytool -genkeypair -v -keystore moka-release.jks -alias moka-release -keyalg RSA -keysize 4096 -validity 10000`.
2. Back up the keystore, alias and passwords securely offline. **Do not attach keystores or passwords in chat, upload them to Git, or paste their contents into issues.**
3. In the GitHub repository, open **Settings → Secrets and variables → Actions** and add:

| Repository secret | Value |
| --- | --- |
| `MOKA_SIGNING_STORE_B64` | Keystore file, encoded in one-line base64 (`base64 -w0 moka-release.jks`; keep output private) |
| `MOKA_SIGNING_STORE_PASSWORD` | Existing keystore password |
| `MOKA_SIGNING_KEY_ALIAS` | Existing alias, e.g. `moka-release` |
| `MOKA_SIGNING_KEY_PASSWORD` | Alias key password |
| `MOKA_SIGNING_CERT_SHA256` | **Public** signing certificate SHA-256 digest, 64 hex digits, no colons |

4. After the signed-workflow PR is merged, open **Actions → Moka Beta 6.1 persistent-key signed APK (manual) → Run workflow**, choose `master`. This workflow never accesses signing secrets from pull requests or untrusted branches.
5. The workflow requires all secrets, builds and tests the app, checks package/version and signed certificate fingerprint, and uploads a **private Actions artifact** with an APK and SHA-256 checksum. It does not automatically create a GitHub Release or publish to a store.
6. Before release, compare fingerprints from two signed builds using `apksigner verify --print-certs`, then test an in-place `adb install -r` on a device previously running an APK signed by that **same certificate**.

## Migration warning

GitHub Actions debug APKs are usually signed with disposable debug certificates. A new release-signed APK **will not update an APK signed with a different certificate** even if the versionCode increases. Do not uninstall without backing up Moka settings you need; Android app data and Keystore keys may be lost.

The older `keystore.properties` local signing method still works. CI uses environment variables only during the explicit signing workflow. Never commit `keystore.properties` or the signing keystore.
