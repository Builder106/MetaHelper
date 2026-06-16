package com.metahelper.app

import android.content.Context
import android.media.AudioManager
import android.util.Log

class VolumeController(
    private val getVolume: () -> Int,
    private val setVolume: (Int) -> Unit,
) {
    // Wires the media-stream read/write to the device. Kept behind the lambdas
    // above so the save/restore state logic is testable on a plain JVM without
    // an AudioManager.
    constructor(context: Context) : this(
        getVolume = {
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .getStreamVolume(AudioManager.STREAM_MUSIC)
        },
        setVolume = { level ->
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        },
    )

    // Remembers the user's media volume from before we forced quiet mode, so we
    // can put it back afterwards instead of leaving their device pinned at 1.
    private var previousVolume: Int? = null

    fun setQuietVolume() {
        try {
            // Capture the current level once (guard against back-to-back captures
            // overwriting it with the already-quieted value).
            if (previousVolume == null) {
                previousVolume = getVolume()
            }
            // Set media volume to a very low level (index 1). This ensures that
            // even if the backend scaling isn't enough, the hardware output is
            // also capped.
            setVolume(1)
            Log.d("VolumeController", "Volume set to quiet (index 1), was $previousVolume")
        } catch (e: Exception) {
            Log.e("VolumeController", "Failed to set volume: ${e.message}")
        }
    }

    fun restoreVolume() {
        try {
            previousVolume?.let {
                setVolume(it)
                Log.d("VolumeController", "Volume restored to $it")
            }
            previousVolume = null
        } catch (e: Exception) {
            Log.e("VolumeController", "Failed to restore volume: ${e.message}")
        }
    }
}

