# Moka Beta 6.1 — signed APK for Uptodown

Status: **release preparation; no Uptodown submission or verified signed APK yet.**

Source: current `master` Beta 6.1; `com.mokamusic.player`; versionCode **39**; versionName **4.0.0-beta06.1-wonhee-preview**. Android 8.0+; `arm64-v8a` and `x86_64`.

The Uptodown upload must be the **release-signed APK**, not an AAB and not a GitHub debug preview. Use your existing persistent Moka signing key. A new key produces a different Android signing identity, and older installations signed by another key cannot update in place. Back up app settings before any uninstall.

## Option A — build locally on Fedora

This option uses the existing private `keystore.properties` in the repository root or the four `MOKA_SIGNING_*` file/password/alias environment variables. Neither the keystore nor passwords belong in Git.

1. Confirm that the existing keystore and alias are the ones intended for all future public Moka APKs. Display its **public certificate fingerprint** locally:

   ```bash
   keytool -list -v -keystore /path/to/YOUR-EXISTING-MOKA-KEY.jks -alias YOUR_ALIAS
   ```

   Copy the certificate SHA-256 fingerprint as exactly 64 hexadecimal characters (remove colons). Do not share the keystore or its passwords.

2. With your existing signing configuration in place, open a shell in the Moka repository and run:

   ```bash
   git fetch origin
   git switch --track origin/release/beta061-uptodown-prep
   export MOKA_SIGNING_CERT_SHA256='YOUR_64_HEX_SIGNING_CERT_FINGERPRINT'
   bash scripts/build-uptodown-apk.sh
   ```

   If the branch already exists locally, switch to it normally and pull with `--ff-only`. Do not overwrite local changes.

3. If successful, find the APK and its checksum under:

   ```text
   dist/uptodown/Moka-Music-Player-4.0.0-beta06.1-wonhee-uptodown-signed.apk
   dist/uptodown/SHA256SUMS.txt
   ```

The script runs tests and `assembleRelease`, rejects a missing/invalid signature, compares the actual signer against your pinned SHA-256 certificate fingerprint, checks package ID/version, and validates 16 KB ZIP alignment when supported. Real native page-size and device tests are separate.

## Option B — manual GitHub Actions signing

The trusted `master` already contains the workflow **Moka Beta 6.1 persistent-key signed APK (manual)**. It does not sign on pull requests.

In the repository **Settings → Secrets and variables → Actions**, privately configure exactly these five secrets using the **same existing release key**:

- `MOKA_SIGNING_STORE_B64` — one-line base64 of the existing keystore file.
- `MOKA_SIGNING_STORE_PASSWORD`
- `MOKA_SIGNING_KEY_ALIAS`
- `MOKA_SIGNING_KEY_PASSWORD`
- `MOKA_SIGNING_CERT_SHA256` — the public SHA-256 certificate digest, 64 hex digits.

Open **Actions → Moka Beta 6.1 persistent-key signed APK (manual) → Run workflow → master**. Download the resulting artifact `moka-beta061-persistent-key-signed-apk` with the versioned APK and `SHA256SUMS.txt`. If any secret is missing or the certificate differs, the job fails and **does not upload** the APK. Keep a backed-up copy of the signed artifact; Actions artifact retention is limited.

Guide: [Persistent signing workflow](MOKA_SIGNED_RELEASE.md).

## Uptodown submission gate

Official instructions: https://support.uptodown.com/hc/en-us/articles/360053260491-How-to-publish-an-app-on-Uptodown

1. Register/use your Uptodown Developers Console publisher profile under the Yooshastan brand, and use **Apps → Add new app** to upload the locally downloaded, verified signed `.apk` file (not an Actions artifact link).
2. Fill category, accurate app description, icon, screenshots, privacy policy, support details, free/ads/IAP declarations and license. See [listing draft](UPTODOWN_LISTING_BETA061.md).
3. Confirm installed APK signature/fingerprint with the exact retained release identity; test Pixel 8a and Samsung A36: local FLAC/WAV, DSP EQ/DDC/convolver, library/queue/shuffle, Bluetooth and USB, Navidrome HTTPS, and `http://10.77.0.1:4533` **only with Wonhee VPN connected**.
4. **Do not submit until** `docs/release/DSP_SOURCE_PROVENANCE.md` is signed off, the dependency-license inventory and notices are complete, privacy policy is live, and native libraries are checked for 16 KB page compatibility. Source-provenance status is currently **OPEN**.
5. On approval, confirm Uptodown's actual listing URL, artifact SHA-256 and signer. Only then link to it from `yooshastan.com/moka/`. Keep personal portfolio/GitHub links off the studio page.

Uptodown accepts APK files, but publication is reviewed by its editors. There is no guarantee of approval or a live listing merely from uploading.

## Signing identity cautions

- A debug APK, a signed release APK and a Play-delivered APK may have different certificates; cross-channel in-place upgrades work only if Android accepts their signing lineage.
- Never publish keystore files, passwords, `keystore.properties`, signed server URLs or Navidrome credentials.
- Do not publish a newly generated public signing identity if an earlier public Moka APK already used a different key without a deliberate migration decision.
- The separate Beta 6.2 DSP preview (versionCode 40) is still unmerged and not the selected Uptodown release candidate. Avoid mixing its debug artifact with Beta 6.1 signed release.

Source-of-truth references: https://developer.android.com/studio/publish/app-signing and https://developer.android.com/guide/practices/page-sizes.
