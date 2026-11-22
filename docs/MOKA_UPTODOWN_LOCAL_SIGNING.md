# Moka laptop signing and Uptodown update gate

The uploaded Beta 6.3 Android package inspected on 2026-11-22 contains:

- Package ID: `com.mokamusic.player`
- Version name: `4.0.0-beta06.3-wonhee-split-preview`
- Version code: `41`
- APK file SHA-256: `f56b328d8e99749d6acc75ef3c658470d0d4b29f3c5984d3ee87b6f7981a21c5`
- Public signer certificate SHA-256: `b61f5999305f84d71b696bdd7ee7eb0e5fdff40d241d8a729f0a98de4ecef0b4`

**Source-provenance note:** These identifiers came from a user-uploaded Moka APK. They do not independently establish that Uptodown currently distributes this exact APK. Confirm by downloading the listing directly and comparing the APK SHA-256 before relying on the signing certificate as the store-wide update identity.

This certificate **differs** from the previously documented older Uptodown Beta 6 certificate `48e894dd6d627a773f08f973f5969f694d8345019d1a48fd774017dec1a2c11d`. Updating Beta 6.3 installs may therefore require a different identity than updating older Beta 6 installs. The local builder intentionally checks only the uploaded Beta 6.3 baseline; don't claim it can update every older build.

## On your Fedora laptop

Use a trusted working clone of `yooshastan/MokaMusicPlayer` and switch to the branch that includes the release helper:

```bash
git clone https://github.com/yooshastan/MokaMusicPlayer.git
cd MokaMusicPlayer
git fetch origin
git switch feature/beta064-local-uptodown-signing
```

If the repository already exists, run `git fetch origin` and `git switch feature/beta064-local-uptodown-signing` inside that repository instead. Do **not** remove or overwrite an existing signing keystore.

Configure the existing signing key by the repository's ignored `keystore.properties` (see `app/build.gradle.kts`) or use the existing `MOKA_SIGNING_STORE_FILE`, `MOKA_SIGNING_STORE_PASSWORD`, `MOKA_SIGNING_KEY_ALIAS`, `MOKA_SIGNING_KEY_PASSWORD` environment variables. Never put secrets into a Git commit, log, chat, or command line.

The local release helper is designed to run **after** the new Moka code is ready and `versionCode` has been increased to at least 42.

```bash
bash -n scripts/build-moka-uptodown-local.sh
bash scripts/build-moka-uptodown-local.sh
```

It performs the following release gates, and refuses to produce an uploadable APK on any failure:

1. Reads Gradle's *release variant* certificate and matches the uploaded Beta 6.3 certificate.
2. Runs local unit tests and builds the signed **release** variant using only the existing key.
3. Confirms Android application ID is `com.mokamusic.player` and `versionCode > 41`.
4. Runs Android SDK `apksigner verify --verbose --print-certs` on the resulting APK, verifying exactly one matching signing certificate.
5. Rejects debuggable releases; packages the APK and its SHA-256 checksum in `dist/uptodown/`.

**Current Beta 6.3 source remains versionCode 41.** The helper is expected to refuse until the actual compatibility release has been implemented, tested and version-bumped. This branch does not publish an APK or access your private keystore.

## For Uptodown

Upload only a **release-signed**, non-debuggable APK that passed the gate, once the store certificate matches the real Uptodown listing. The debug-signed GitHub CI APK must not be submitted as an in-place store update. If the store's verified signer differs from the key available on your laptop, do not create a new key and pretend it can update existing installs; recover the original key or plan a separately communicated migration.
