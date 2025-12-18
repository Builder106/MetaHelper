package com.metahelper.app

import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.  Wearables
import com.meta.wearable.dat.camera.StreamSession
import com.meta.wearable.dat.camera.startStreamSession
import com.meta.wearable.dat.camera.types.PhotoData
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/**
 * Manager class that coordinates the flow between the glasses and the backend.
 * Updated for Meta Wearables SDK 0.3.0 using the StreamSession API.
 */
class GlassesManager(
    private val context: Context,
    private val backendUrl: String = "http://172.21.100.50:8000"
) {
    private val apiClient = ApiClient(backendUrl)
    private val audioPlayer = AudioPlayer(context)
    private val volumeController = VolumeController(context)
    
    private var streamSession: StreamSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        // MANDATORY: Initialize the SDK (0.3.0)
        val result = Wearables.initialize(context)
        if (result.isSuccess) {
            Log.d("GlassesManager", "Meta Wearables SDK 0.3.0 Initialized")
            startSession()
        } else {
            Log.e("GlassesManager", "SDK Initialization failed: ${result.exceptionOrNull()?.message}")
        }
    }

    private fun startSession() {
        Log.d("GlassesManager", "Initializing session with AutoDeviceSelector...")

        // 0.3.0: AutoDeviceSelector is ideal for single-device setups as it 
        // maintains selection across availability changes.
        // We use a simple comparator that prioritizes any detected device.
        val deviceSelector = com.meta.wearable.dat.core.selectors.AutoDeviceSelector { _, _ -> 0 }

        // The session will automatically transition to STREAMING when the glasses are found
        streamSession = Wearables.startStreamSession(context, deviceSelector)

        // Monitor session state
        streamSession?.let { session ->
            serviceScope.launch {
                session.state.collect { state ->
                    Log.d("GlassesManager", "Session state changed to: $state")
                }
            }
        }
    }

    private fun convertBitmapToByteArray(bitmap: android.graphics.Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Trigger a photo capture using the StreamSession.capturePhoto suspend function.
     */
    fun triggerPhotoCapture() {
        val session = streamSession ?: run {
            Log.e("GlassesManager", "StreamSession not initialized.")
            return
        }

        serviceScope.launch {
            Log.d("GlassesManager", "Capturing photo via StreamSession...")
            // 0.3.0 Guideline: capturePhoto is a suspend function returning Result<PhotoData>
            val result = session.capturePhoto()

            if (result.isSuccess) {
                val photoData = result.getOrThrow()
                when (photoData) {
                    is PhotoData.HEIC -> {
                        val bytes = photoData.data.array() // Extract HEIC data as ByteArray
                        Log.d("GlassesManager", "Photo captured in HEIC format (${bytes.size} bytes). Processing...")
                        onPhotoCaptured(bytes)
                    }
                    is PhotoData.Bitmap -> {
                        val bitmap = photoData.bitmap // Extract Android Bitmap
                        Log.d("GlassesManager", "Photo captured as Bitmap (${bitmap.width}x${bitmap.height}). Processing...")
                        // Here you can save or process the bitmap (e.g., converting to JPEG or PNG)
                        val bytes = convertBitmapToByteArray(bitmap) // Convert bitmap to ByteArray
                        onPhotoCaptured(bytes)
                    }
                    else -> {
                        Log.e("GlassesManager", "Unsupported PhotoData format: ${photoData.javaClass.name}")
                    }
                }
            } else {
                Log.e("GlassesManager", "Capture failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun onPhotoCaptured(imageBytes: ByteArray) {
        volumeController.setQuietVolume()
        apiClient.processImage(imageBytes, object : ApiClient.ApiResponseCallback {
            override fun onSuccess(audioBytes: ByteArray) {
                Log.d("GlassesManager", "Playing AI response...")
                audioPlayer.playAudio(audioBytes)
            }
            override fun onError(message: String) {
                Log.e("GlassesManager", "Pipeline Error: $message")
            }
        })
    }

    fun stopAll() {
        audioPlayer.stop()
        streamSession?.close()
        serviceScope.cancel()
    }
}
