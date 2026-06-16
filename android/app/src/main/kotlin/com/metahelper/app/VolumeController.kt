package com.metahelper.app

import android.content.Context
import android.media.AudioManager
import android.util.Log

class VolumeController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Remembers the user's media volume from before we forced quiet mode, so we
    // can put it back afterwards instead of leaving their device pinned at 1.
    private var previousVolume: Int? = null

    fun setQuietVolume() {
        try {
            // Capture the current level once (guard against back-to-back captures
            // overwriting it with the already-quieted value).
            if (previousVolume == null) {
                previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }
            // Set media volume to a very low level (index 1)
            // This ensures that even if the backend scaling isn't enough,
            // the hardware output is also capped.
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                1, // Lowest audible level
                0
            )
            Log.d("VolumeController", "Volume set to quiet (index 1), was $previousVolume")
        } catch (e: Exception) {
            Log.e("VolumeController", "Failed to set volume: ${e.message}")
        }
    }

    fun restoreVolume() {
        try {
            previousVolume?.let {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0)
                Log.d("VolumeController", "Volume restored to $it")
            }
            previousVolume = null
        } catch (e: Exception) {
            Log.e("VolumeController", "Failed to restore volume: ${e.message}")
        }
    }
}

