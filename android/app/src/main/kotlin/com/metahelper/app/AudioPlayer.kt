package com.metahelper.app

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(audioBytes: ByteArray, onComplete: () -> Unit = {}) {
        try {
            // Stop any existing playback
            stop()

            // Create a temporary file to play from
            val tempFile = File.createTempFile("response", "mp3", context.cacheDir)
            tempFile.deleteOnExit()
            val fos = FileOutputStream(tempFile)
            fos.write(audioBytes)
            fos.close()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    onComplete()
                    tempFile.delete()
                    stop()
                }
                start()
            }
            Log.d("AudioPlayer", "Started playing audio response")
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error playing audio: ${e.message}")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}

