package com.metahelper.app

import android.content.Context
import android.util.Log

/**
 * Manager class that coordinates the flow between the glasses and the backend.
 * This is where the Meta Wearables SDK integration would live.
 */
class GlassesManager(
    private val context: Context,
    private val backendUrl: String = "http://10.0.2.2:8000" // Default for Android Emulator
) {
    private val apiClient = ApiClient(backendUrl)
    private val audioPlayer = AudioPlayer(context)
    private val volumeController = VolumeController(context)

    /**
     * Call this when the Meta SDK notifies that a photo has been taken.
     * @param imageBytes The raw photo data from the glasses.
     */
    fun onPhotoCaptured(imageBytes: ByteArray) {
        Log.d("GlassesManager", "Photo captured, processing...")

        // 1. Ensure volume is quiet before playing anything
        volumeController.setQuietVolume()

        // 2. Send to backend
        apiClient.processImage(imageBytes, object : ApiClient.ApiResponseCallback {
            override fun onSuccess(audioBytes: ByteArray) {
                Log.d("GlassesManager", "Received audio response, playing...")
                
                // 3. Play the quiet audio response
                audioPlayer.playAudio(audioBytes)
            }

            override fun onError(message: String) {
                Log.e("GlassesManager", "Error processing image: $message")
                // Optional: Play a quiet error beep or notification
            }
        })
    }

    fun stopAll() {
        audioPlayer.stop()
    }
}

