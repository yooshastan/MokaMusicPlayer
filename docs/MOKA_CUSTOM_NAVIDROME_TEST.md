# Moka 4.0.0 Beta 6.3 — user-configured Navidrome test candidate

This candidate removes the hardcoded `10.77.0.1:4533` limitation.
It accepts arbitrary HTTPS Navidrome URLs and user-chosen **private numeric
HTTP IP:port** destinations inside RFC1918, 100.64.0.0/10, or IPv6 ULA.
Public HTTP and HTTP DNS hostnames remain forbidden by the Navidrome parser.

## Important safety constraints

- Dynamic private-IP HTTP needs an Android cleartext base configuration.
  All Navidrome HTTP paths must still pass app-level URL policy and use
  exactly one Android-recognized VPN `Network`, selected by the longest
  matching route. The app must not silently retry on the default network.
- HTTPS is strongly preferred because WireGuard protects transport only,
  not end-to-end HTTP server identity.
- **Kernel-only** WireGuard interfaces that Android does not expose as a VPN
  Network will fail closed. The existing rooted Wonhee multi-tunnel mode is
  not assumed to be an Android `TRANSPORT_VPN`. Do not relax this gate
  until a safe socket-binding mechanism and disconnection test exist.
- Device testing, dependency/security review, signing identity continuity
  and store metadata checks are prerequisites to public distribution.

## One consolidated test

1. Preserve the currently installed store version and its login settings;
   preview/debug APK signing may not match Uptodown's certificate.
2. With VPN connected, try your own HTTPS Navidrome URL and verify sign-in,
   album browsing, shuffle, artwork and FLAC/WAV playback.
3. With a suitable Android-recognized VPN, use a numeric private HTTP
   endpoint, e.g. `http://10.0.0.169:4533` (custom port accepted).
4. Confirm library requests and playback do not leave that selected VPN,
   using a bounded tcpdump capture on the actual tunnel interface.
5. Turn off the selected VPN and retry connect/playback. Expect refusal,
   no Wi-Fi/cellular TCP attempt to the HTTP address, and no HTTP redirects.
6. Test a public `http://` URL and an HTTP hostname; both must be refused.
7. Verify that unrelated HTTPS metadata/artwork services remain working.

If step 3 is rejected because Android does not expose a VPN Network, that is
an **expected safe failure**, not a successful compatibility fix. Return the
exact error so the next iteration can address rooted kernel WireGuard
without weakening app-side fail-closed semantics.
