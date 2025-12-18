package com.metahelper.app

import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.RegistrationState
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.camera.StreamSession
import com.meta.wearable.dat.camera.startStreamSession
import com.meta.wearable.dat.camera.types.PhotoData
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/**
 * Manager class that coordinates the flow between the glasses and the backend.
 * Fully aligned with Meta Wearables SDK 0.3.0 Documentation.
 */
class GlassesManager(
    private val context: Context,
    private val backendUrl: String = "https://metahelper.onrender.com"
) {
    private val apiClient = ApiClient(backendUrl)
    private val audioPlayer = AudioPlayer(context).apply {
        // Double-tap shortcut for repeating the last explanation
        onReplayRequested = { replayLastAudio() }
    }
    private val volumeController = VolumeController(context)
    private var lastAudioResponse: ByteArray? = null
    
    private var streamSession: StreamSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        // Initialize SDK as per com_meta_wearable_dat_core_wearables#initialize
        val result = Wearables.initialize(context)
        if (result.isSuccess) {
            Log.d("GlassesManager", "Meta Wearables SDK 0.3.0 Initialized")
            checkRegistrationAndStart()
        } else {
            Log.e("GlassesManager", "SDK Initialization failed")
        }
    }

    private fun checkRegistrationAndStart() {
        serviceScope.launch {
            // Observe registrationState StateFlow
            Wearables.registrationState.collect { state ->
                when (state) {
                    is RegistrationState.Registered -> {
                        Log.d("GlassesManager", "App is REGISTERED. Starting session...")
                        startSession()
                    }
                    is RegistrationState.Available -> {
                        Log.d("GlassesManager", "App is UNREGISTERED. Opening Meta AI...")
                        Wearables.startRegistration(context)
                    }
                    else -> Log.d("GlassesManager", "Registration state: $state")
                }
            }
        }
    }

    private fun startSession() {
        Log.d("GlassesManager", "Initializing session...")
        
        // Use AutoDeviceSelector as per com_meta_wearable_dat_core_selectors_autodeviceselector
        val deviceSelector = AutoDeviceSelector { _, _ -> 0 }
        
        // Start session using the extension function in com.meta.wearable.dat.camera
        streamSession = Wearables.startStreamSession(context, deviceSelector)

        serviceScope.launch {
            streamSession?.state?.collect { state ->
                Log.d("GlassesManager", "StreamSession state: $state")
            }
        }
    }

    /**
     * Trigger a photo capture using the StreamSession API.
     * This captures the POV image from the glasses.
     */
    fun triggerPhotoCapture() {
        val session = streamSession ?: run {
            Log.e("GlassesManager", "Session not active.")
            return
        }

        serviceScope.launch {
            Log.d("GlassesManager", "Capturing photo...")
            // As per StreamSession interface: capturePhoto() returns Result<PhotoData>
            val result = session.capturePhoto()
            
            if (result.isSuccess) {
                val photoData = result.getOrThrow()
                processPhotoData(photoData)
            } else {
                Log.e("GlassesManager", "Capture failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun processPhotoData(photoData: PhotoData) {
        when (photoData) {
            is PhotoData.HEIC -> {
                Log.d("GlassesManager", "Processing HEIC photo")
                onPhotoCaptured(photoData.data.array())
            }
            is PhotoData.Bitmap -> {
                Log.d("GlassesManager", "Processing Bitmap photo")
                val outputStream = ByteArrayOutputStream()
                photoData.bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                onPhotoCaptured(outputStream.toByteArray())
            }
            else -> Log.e("GlassesManager", "Unknown PhotoData type")
        }
    }

    private fun onPhotoCaptured(imageBytes: ByteArray) {
        volumeController.setQuietVolume()
        apiClient.processImage(imageBytes, object : ApiClient.ApiResponseCallback {
            override fun onSuccess(audioBytes: ByteArray) {
                lastAudioResponse = audioBytes
                Log.d("GlassesManager", "AI Solution ready. Playing...")
                audioPlayer.playAudio(audioBytes)
            }
            override fun onError(message: String) {
                Log.e("GlassesManager", "Pipeline Error: $message")
            }
        })
    }

    fun replayLastAudio() {
        lastAudioResponse?.let {
            Log.d("GlassesManager", "Replaying last explanation...")
            audioPlayer.playAudio(it)
        }
    }

    fun stopAll() {
        audioPlayer.release()
        streamSession?.close() // As per StreamSession interface
        serviceScope.cancel()
    }
}
