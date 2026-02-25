package com.example.pager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class SoundAlarmManager(private val context: Context) {

    companion object {
        private const val TAG = "SoundAlarmManager"

        @Volatile
        private var instance: SoundAlarmManager? = null

        fun getInstance(context: Context): SoundAlarmManager {
            return instance ?: synchronized(this) {
                instance ?: SoundAlarmManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var audioQueue: List<String> = emptyList()
    private var currentAudioIndex = 0
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var pendingTTSText: String? = null

    /**
     * Plays a sequence of audio files by name.
     * Audio files must be located in res/raw/ directory.
     * @param audioFiles List of audio file names (e.g., listOf("intro.m4a", "message.m4a"))
     * @param volume Volume level (0.0 to 1.0). Default is 1.0 (maximum).
     */
    fun playMessage(audioFiles: List<String>, volume: Float = 1.0f) {
        if (audioFiles.isEmpty()) {
            Log.w(TAG, "Audio files list is empty")
            return
        }

        stopSound()
        audioQueue = audioFiles
        currentAudioIndex = 0

        Log.d(TAG, "Starting playback of ${audioFiles.size} audio files")
        playNextAudio(volume)
    }

    /**
     * Plays text using text-to-speech.
     * @param text The text to speak.
     * @param volume Volume level (0.0 to 1.0). Default is 1.0 (maximum).
     */
    fun playTTSVoice(text: String, volume: Float = 1.0f) {
        if (text.isBlank()) {
            Log.w(TAG, "TTS text is empty")
            return
        }

        stopSound()

        Log.d(TAG, "Playing TTS: $text")

        if (!ttsInitialized) {
            Log.d(TAG, "Initializing TextToSpeech engine")
            pendingTTSText = text
            initializeTTS(volume)
        } else {
            speakText(text, volume)
        }
    }

    private fun initializeTTS(volume: Float) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "TTS language not supported")
                        ttsInitialized = false
                    } else {
                        ttsInitialized = true
                        Log.d(TAG, "TTS initialized successfully")

                        // Play pending text if any
                        pendingTTSText?.let { text ->
                            speakText(text, volume)
                            pendingTTSText = null
                        }
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                ttsInitialized = false
            }
        }
    }

    private fun speakText(text: String, volume: Float) {
        textToSpeech?.let { tts ->
            // Set audio attributes for TTS
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            tts.setAudioAttributes(audioAttributes)

            // Set utterance progress listener
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started speaking")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed")
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error")
                }
            })

            val params = Bundle()
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_utterance")
        } ?: run {
            Log.e(TAG, "TextToSpeech not initialized")
        }
    }

    private fun playNextAudio(volume: Float) {
        if (currentAudioIndex >= audioQueue.size) {
            Log.d(TAG, "Audio sequence completed")
            audioQueue = emptyList()
            return
        }

        val fileName = audioQueue[currentAudioIndex]
        val resourceId = getResourceIdByName(fileName)

        if (resourceId == 0) {
            Log.e(TAG, "Audio file not found: $fileName")
            currentAudioIndex++
            handler.post { playNextAudio(volume) }
            return
        }

        try {
            // Ensure previous player is fully released
            mediaPlayer?.release()
            mediaPlayer = null

            Log.d(TAG, "Creating MediaPlayer for: $fileName ($resourceId)")

            // Use constructor instead of create() for better control
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )

                setOnCompletionListener {
                    Log.d(TAG, "Completed playing: $fileName")
                    try {
                        release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing MediaPlayer", e)
                    }
                    mediaPlayer = null
                    currentAudioIndex++

                    // Post to handler with delay for cleanup
                    handler.postDelayed({ playNextAudio(volume) }, 300)
                }

                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for file: $fileName")
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing failed MediaPlayer", e)
                    }
                    mediaPlayer = null
                    currentAudioIndex++
                    handler.postDelayed({ playNextAudio(volume) }, 300)
                    true
                }

                val afd = context.resources.openRawResourceFd(resourceId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                prepare()
                setVolume(volume, volume)
                start()
            }

            Log.d(TAG, "Playing audio file: $fileName ($resourceId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: $fileName", e)
            mediaPlayer?.release()
            mediaPlayer = null
            currentAudioIndex++
            handler.postDelayed({ playNextAudio(volume) }, 300)
        }
    }

    private fun getResourceIdByName(fileName: String): Int {
        val resourceName = fileName.substringBeforeLast(".").lowercase()
        return context.resources.getIdentifier(
            resourceName,
            "raw",
            context.packageName
        )
    }

    /**
     * Stops any currently playing audio.
     */
    fun stopSound() {
        try {
            handler.removeCallbacksAndMessages(null)
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            audioQueue = emptyList()
            currentAudioIndex = 0

            // Stop TTS if speaking
            textToSpeech?.stop()

            Log.d(TAG, "Audio playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    /**
     * Checks if audio is currently playing.
     */
    fun isPlaying(): Boolean {
        val mediaPlayerPlaying = mediaPlayer?.isPlaying ?: false
        val ttsPlaying = textToSpeech?.isSpeaking ?: false
        return mediaPlayerPlaying || ttsPlaying
    }

    /**
     * Clean up resources. Call this when the manager is no longer needed.
     */
    fun cleanup() {
        stopSound()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsInitialized = false
        Log.d(TAG, "SoundAlarmManager cleaned up")
    }
}

