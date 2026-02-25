package com.example.pager

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
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
        @SuppressLint("StaticFieldLeak")
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
    private var pendingVolume: Float = 1.0f
    private var pendingSpeechRate: Float = 1.0f
    private var selectedVoice: String? = null

    fun setTTSVoice(voiceName: String) {
        selectedVoice = voiceName
        if (ttsInitialized) {
            applyVoiceSelection()
        }
    }

    private fun applyVoiceSelection() {
        textToSpeech?.voices?.find { it.name == selectedVoice }?.let { voice ->
            val result = textToSpeech?.setVoice(voice)
            if (result == TextToSpeech.SUCCESS) {
                Log.d(TAG, "Voice set to: ${voice.name} (${voice.locale.displayName})")
            } else {
                Log.e(TAG, "Failed to set voice: ${voice.name}")
            }
        } ?: run {
            Log.w(TAG, "Voice not found: $selectedVoice")
        }
    }

    /**
     * Plays text using text-to-speech.
     * @param text The text to speak.
     * @param volume Volume level (0.0 to 1.0). Default is 1.0 (maximum).
     * @param speechRate Speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed). Default is 1.0.
     */
    fun playTTSVoice(text: String, volume: Float = 1.0f, speechRate: Float = 1.0f) {
        if (text.isBlank()) {
            Log.w(TAG, "TTS text is empty")
            return
        }
        stopSound()
        Log.d(TAG, "Playing TTS: $text (rate: $speechRate)")
        if (!ttsInitialized) {
            Log.d(TAG, "Initializing TextToSpeech engine")
            pendingTTSText = text
            initializeTTS(volume, speechRate)
        } else {
            speakText(text, volume, speechRate)
        }
    }

    private fun initializeTTS(volume: Float, speechRate: Float) {
        pendingVolume = volume
        pendingSpeechRate = speechRate
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
                        if (selectedVoice != null) {
                            applyVoiceSelection()
                        } else {
                            selectBestVoice()
                        }
                        tts.voices?.forEach { voice ->
                            Log.d(TAG, "Available voice: ${voice.name}, Quality: ${voice.quality}, Locale: ${voice.locale}")
                        }
                        pendingTTSText?.let { text ->
                            speakText(text, pendingVolume, pendingSpeechRate)
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

    private fun selectBestVoice() {
        textToSpeech?.voices
            ?.filter { it.locale.language == "en" && it.locale.country == "US" }
            ?.maxByOrNull { it.quality }
            ?.let { bestVoice ->
                textToSpeech?.voice = bestVoice
                Log.d(TAG, "Selected best voice: ${bestVoice.name} (Quality: ${bestVoice.quality})")
            }
    }

    private fun speakText(text: String, volume: Float, speechRate: Float) {
        textToSpeech?.let { tts ->
            tts.setSpeechRate(speechRate)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            tts.setAudioAttributes(audioAttributes)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started speaking")
                }
                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed")
                }
                @Deprecated("Deprecated in Java")
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
            textToSpeech?.stop()
            Log.d(TAG, "Audio playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio", e)
        }
    }

    fun cleanup() {
        stopSound()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsInitialized = false
        Log.d(TAG, "SoundAlarmManager cleaned up")
    }
}

