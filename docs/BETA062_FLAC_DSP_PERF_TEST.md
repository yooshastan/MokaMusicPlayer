# Beta 6.2 — FLAC/DSP latency and library regression test

This preview **does not change DSP math, codecs, or buffer sizes**. It prevents a full audio-chain rebuild when a saved DSP profile has the same effective audio settings, and emits one `MokaTransition` warning after five seconds if a track still has not started. The data will guide further performance fixes without destabilizing the verified Beta 6.1 release.

## Test matrix

On the Pixel 8a and Samsung A36, compare a familiar local FLAC and a Navidrome FLAC through Wonhee:

1. DSP fully off, local FLAC: measure first audible sample after tap; note stutter.
2. DSP on with EQ only: measure first sound and switching between tracks.
3. DSP on with saved VDC/DDC and convolution IRS: compare first sound, queue transitions, and seek.
4. While playing, repeatedly save an **identical** DSP profile or rename a display label; the track must **not** restart or emit new `dsp-route-rebuild` events.
5. Move one EQ slider, enable/disable DSP, change an actual VDC/IRS: audio must update without crashes; if direct float playback is active it should report the existing hot-reload path.
6. Shuffle local and Navidrome tracks, navigate Settings, and switch Wi-Fi to cellular under Wonhee. All network streams must remain VPN-bound; local files should play normally without VPN.
7. Check the UI's Audio Path/DSP diagnostics: underrun counts, queue depth, primed frames, throughput multiplier, and transition times.

## Collect safe logs

Run from Fedora while USB debugging the phone:

```bash
adb logcat -c
adb logcat -v threadtime -s MokaTransition MokaAudio MokaHybrid MokaNativeDSP MokaFocus > moka-beta062-playback.log
```

Play three tracks each in the above configurations, then stop logcat with Ctrl+C.

- Look for `slow-start elapsedMs=` warnings. The warning fires at 5 seconds even when the track never becomes audible.
- For each transition compare `ready elapsedMs` with `audio-start totalMs` to distinguish decoder/DSP preparation from AudioTrack output delay.
- If stuttering persists, note the log timestamp and reported underrun count.

**Before sharing:** review and redact all personal file names, tokens, signed Subsonic stream URLs, IPs and private metadata. Do not share keystores, WireGuard profiles or credentials.

## Exit criteria

- No repeat restart on unchanged DSP saves.
- No regression in direct PCM/Media3 queue handoff, USB output, local library scan, or Navidrome login.
- No claim that the underlying FLAC startup delay is solved until the logs and real-device comparison demonstrate it.
