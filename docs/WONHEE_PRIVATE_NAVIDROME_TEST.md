# Moka Beta 6.1 — Wonhee private Navidrome device test

This preview allows **only** `http://10.77.0.1:4533` as a cleartext Navidrome server. All other Navidrome servers must use HTTPS. The HTTP connection is only protected while it is tunneled through the trusted, active WireGuard VPN; it does **not** provide independent TLS authentication.

## Prerequisites

- Wonhee VPN active on the phone. The server is `10.77.0.1`, the phone `10.77.0.2`.
- On Fedora, Navidrome responds on `127.0.0.1:4533`, and a separate listener/forwarder responds on **`10.77.0.1:4533`**, reachable through the `wonheevpn` firewalld zone.
- Test `http://10.77.0.1:4533` in the phone browser while Wonhee is connected before diagnosing Moka.
- This branch's GitHub Actions debug APK may be signed differently than your installed APK. Back up settings before uninstalling anything.

## Checklist on Pixel 8a and Samsung A36

1. Keep the known-good Beta 6 configuration available. Connect Wonhee.
2. In **Settings → Navidrome**, enter `http://10.77.0.1:4533`, username and password; select **Remember login**.
3. Connect. Verify Albums, Tracks, Artists, Genres, searching and paging. Test the stream probe.
4. Play an original FLAC and a WAV track. Validate sample rate/bit depth where metadata is available; test audio pause/seek, headphones and background playback.
5. Mix local and network tracks into shuffle. Switch phone from Wi-Fi to cellular while keeping Wonhee active; reconnect if necessary and verify streams recover without bypass.
6. Disable Wonhee or switch to a VPN without a route to `10.77.0.1`. Try a new track/library refresh. **Expected: explicit connection failure**. No direct Wi-Fi/cellular HTTP to the private endpoint.
7. Reconnect Wonhee and retry the library and audio. Force-close and restart Moka; confirm encrypted saved login is restored.
8. Independently reconnect using an existing **HTTPS** server; verify the previous behavior is unchanged. Test local files without a VPN.
9. If a playback failure persists, capture logcat without sharing usernames, passwords, Subsonic stream URLs or tokens. Check the Fedora listener, `sudo ss -lntp | grep :4533`, and `wg show`; on the server a stream test can use `sudo tcpdump -ni wg0 'host 10.77.0.2 and tcp port 4533'`.

## Known constraints

- The private address and port are intentionally fixed. Arbitrary HTTP Navidrome URLs, router LAN IPs, redirects, and public cleartext servers are unsupported.
- A `socat` process started manually will not survive a reboot or process exit; the server's listener must remain running for remote access.
- Android normally permits only one active VPN service at a time. Do not expect Tailscale VPN access to work simultaneously with Wonhee; an HTTPS server on the public internet remains supported without Tailscale.
- This preview requires manual real-device tests; CI compilation and unit tests alone cannot prove VPN isolation during reconnect/disconnect.
