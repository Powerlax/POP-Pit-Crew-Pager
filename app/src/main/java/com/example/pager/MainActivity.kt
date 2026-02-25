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
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.util.Log
import com.example.pager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var soundAlarmManager: SoundAlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Initialize sound alarm manager
        soundAlarmManager = SoundAlarmManager.getInstance(this)

        // Listen for Google Messages notifications via notification listener
        AppNotificationListener.onMessageReceived = { sender, messageBody ->
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
                    soundAlarmManager.setTTSVoice("en-us-x-iom-local");
                    soundAlarmManager.playTTSVoice(audioList, volume = 1.0f, speechRate = 1.15f)
                } else {
                    Log.e(TAG, "Received message with invalid format: $messageBody")
                    soundAlarmManager.playTTSVoice("Read with bad format", volume = 1.0f, speechRate = 1.15f);
                }
            }
        }

        if (!isNotificationListenerEnabled()) {
            Snackbar.make(binding.root, "Enable notification access for Pager", Snackbar.LENGTH_LONG)
                .setAction("Enable") {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setAnchorView(R.id.fab)
                .show()
        }

        binding.fab.setOnClickListener { view ->
            // Test each voice sequentially
            val eng_voices = listOf(
                "en-us-x-sfg-local",
                "en-us-x-iom-local",
                "en-us-x-tpc-local",
            )

            Snackbar.make(view, "Testing ${eng_voices.size} voices...", Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.fab).show()

            testVoicesSequentially(eng_voices, 0)
        }

        binding.fab.setOnLongClickListener { view ->
            // List available TTS voices
            val voices = soundAlarmManager.getAvailableVoices()
            if (voices.isNotEmpty()) {
                Log.d(TAG, "=== Available TTS Voices ===")
                voices.forEachIndexed { index, voice ->
                    Log.d(TAG, "${index + 1}. $voice")
                }
                Snackbar.make(view, "Listed ${voices.size} voices in logs", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .show()
            } else {
                Snackbar.make(view, "No voices available yet. Play TTS first.", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.fab)
                    .show()
            }
            true
        }
    }

    private fun testVoicesSequentially(voices: List<String>, index: Int) {
        if (index >= voices.size) {
            Log.d(TAG, "=== Voice testing completed ===")
            Snackbar.make(binding.root, "Voice testing completed!", Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.fab).show()
            return
        }

        val voiceName = voices[index]
        Log.d(TAG, "Testing voice ${index + 1}/${voices.size}: $voiceName")

        soundAlarmManager.setTTSVoice(voiceName)
        soundAlarmManager.playTTSVoice("Voice ${index + 1}: Attention POP pit crew! Please report to the practice field as there is a emergency with the robot. Specifically, There is a Severity 1 electrical issue with the indexer. There is a Severity 2 mechanical issue with the drivetrain.", volume = 1.0f, speechRate = 1.15f)

        // Wait 4 seconds before testing next voice
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            testVoicesSequentially(voices, index + 1)
        }, 20000)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
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
