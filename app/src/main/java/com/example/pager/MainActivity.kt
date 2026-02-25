package com.example.pager

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import android.util.Log
import com.example.pager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "PagerPrefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var soundAlarmManager: SoundAlarmManager
    private var notificationsEnabled = true

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        Log.d(TAG, "Notifications ${if (enabled) "enabled" else "disabled"}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)

        soundAlarmManager = SoundAlarmManager.getInstance(this)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        Log.d(TAG, "Loaded notification setting: $notificationsEnabled")

        // Main handling for parsing messages and triggering audio
        AppNotificationListener.onMessageReceived = messageHandler@{ sender, messageBody ->
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled - ignoring message from $sender")
                return@messageHandler
            }
            Log.d(TAG, "Message notification - Sender: $sender, Body: $messageBody")

            val tokens = messageBody.split("-")
            var audioList = ""
            for (token in tokens) {
                Log.d(TAG, "Token: '${token.trim()}'")
            }
            if (tokens.size > 2) {
                audioList += "Attention Pack of Parts pit crew! Please report to the "
                audioList += when (tokens[0].trim().lowercase()) {
                    "pf" -> "practice field"
                    "mf" -> "main field"
                    "p" -> "pit"
                    else -> "BAD_REQUEST"
                }
                audioList += " as there is a "
                audioList += when (tokens[1].trim().lowercase()) {
                    "e" -> "emergency"
                    "m" -> "medium issue"
                    "s" -> "small issue"
                    else -> "BAD_REQUEST"
                }
                audioList += " with the robot. "
                if (tokens.size >= 5) audioList += "Specifically, "
                var i = 2
                while (i + 2 < tokens.size) {
                    audioList += "There is a "
                    val subsystem = tokens[i].trim().lowercase()
                    val subteam = tokens[i + 1].trim().lowercase()
                    val severity = tokens[i + 2].trim().lowercase()
                    audioList +=
                        when (severity) {
                            "1" -> "Severity 1"
                            "2" -> "Severity 2"
                            "3" -> "Severity 3"
                            "4" -> "Severity 4"
                            "5" -> "Severity 5"
                            else -> "BAD_REQUEST"
                        }
                    audioList += " "
                    audioList +=
                        when (subteam) {
                            "e" -> "electrical"
                            "m" -> "mechanical"
                            "p" -> "programming"
                            "a" -> "all hands"
                            else -> "BAD_REQUEST"
                        }
                    audioList += " issue with the "
                    audioList +=
                        when (subsystem) {
                            "ind" -> "indexer"
                            "shtr" -> "launcher"
                            "clmb" -> "climb"
                            "dt" -> "drivetrain"
                            "int" -> "intake"
                            "hand" -> "handoff"
                            else -> "BAD_REQUEST"
                        }
                    audioList += ". "
                    i += 3
                }
                if (i < tokens.size - 1) {
                    audioList += " Logan says: " + tokens[tokens.size-1]
                }
                for (audio in audioList) {
                    Log.d(TAG, "Audio to play: $audio")
                }
                if ("BAD_REQUEST" !in audioList) {
                    // soundAlarmManager.playMessage(audioList)
                    soundAlarmManager.setTTSVoice("en-us-x-iom-local")
                    soundAlarmManager.playTTSVoice(audioList, volume = 1.0f, speechRate = 1.15f)
                } else {
                    Log.e(TAG, "Received message with invalid format: $messageBody")
                    soundAlarmManager.playTTSVoice("Read with bad format", volume = 1.0f, speechRate = 1.15f)
                }
            }
        }

        if (!isNotificationListenerEnabled()) {
            Snackbar.make(binding.root, "Enable notification access for Pager", Snackbar.LENGTH_LONG)
                .setAction("Enable") {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .show()
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val listenerComponent = ComponentName(this, AppNotificationListener::class.java)
        return enabledListeners.split(":").any { componentString ->
            ComponentName.unflattenFromString(componentString) == listenerComponent
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundAlarmManager.cleanup()
    }
}
