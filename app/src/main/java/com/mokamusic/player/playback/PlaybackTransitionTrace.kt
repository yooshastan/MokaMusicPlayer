package com.mokamusic.player.playback

import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log

/** Logcat: adb logcat -s MokaTransition MokaFocus MokaAudio MokaHybrid */
internal class PlaybackTransitionTrace {
    private var startedMs = -1L
    private var readyMs = -1L
    private var sequence = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val slowWarning = Runnable {
        if (startedMs >= 0) {
            val waitMs = SystemClock.elapsedRealtime() - startedMs
            Log.w(TAG, "transition#$sequence slow-start elapsedMs=$waitMs ready=${readyMs >= 0}")
        }
    }

    fun begin(reason: String, mediaId: String) {
        handler.removeCallbacks(slowWarning)
        sequence++
        startedMs = SystemClock.elapsedRealtime()
        readyMs = -1L
        Log.i(TAG, "transition#$sequence begin reason=$reason item=$mediaId")
        handler.postDelayed(slowWarning, 5_000L)
    }

    fun ready(engine: String) {
        if (startedMs < 0 || readyMs >= 0) return
        readyMs = SystemClock.elapsedRealtime()
        Log.i(TAG, "transition#$sequence ready engine=$engine elapsedMs=${readyMs - startedMs}")
    }

    fun audible(engine: String) {
        if (startedMs < 0) return
        val elapsed = SystemClock.elapsedRealtime() - startedMs
        val afterReady = if (readyMs >= 0) elapsed - (readyMs - startedMs) else -1L
        handler.removeCallbacks(slowWarning)
        Log.i(TAG, "transition#$sequence audio-start engine=$engine totalMs=$elapsed afterReadyMs=$afterReady")
        startedMs = -1L
        readyMs = -1L
    }

    fun close() {
        handler.removeCallbacks(slowWarning)
        startedMs = -1L
        readyMs = -1L
    }

    private companion object {
        const val TAG = "MokaTransition"
    }
}
