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
            val audioList = mutableListOf<String>()
            for (token in tokens) {
                Log.d(TAG, "Token: '${token.trim()}'")
            }
            if (tokens.size > 2) {
                audioList.add("intro.m4a")
                audioList.add(
                    when (tokens[0].trim().lowercase()) {
                        "pf" -> "practice_field.m4a"
                        "mf" -> "main_field.m4a"
                        "p" -> "pit.m4a"
                        else -> "BAD_REQUEST"
                    }
                )
                audioList.add("there_is_a.m4a")
                audioList.add(
                    when (tokens[1].trim().lowercase()) {
                        "e" -> "emergency.m4a"
                        "m" -> "medium_issue.m4a"
                        "s" -> "small_issue.m4a"
                        else -> "BAD_REQUEST"
                    }
                )
                audioList.add("with_the_robot.m4a")
                if (tokens.size >= 5) audioList.add("specifically.m4a")
                var i = 2
                while (i + 2 < tokens.size) {
                    audioList.add("there_is_a.m4a")
                    val subsystem = tokens[i].trim().lowercase()
                    val subteam = tokens[i + 1].trim().lowercase()
                    val severity = tokens[i + 2].trim().lowercase()
                    audioList.add(
                        when (severity) {
                            "1" -> "sev_1.m4a"
                            "2" -> "sev_2.m4a"
                            "3" -> "sev_3.m4a"
                            "4" -> "sev_4.m4a"
                            "5" -> "sev_5.m4a"
                            else -> "BAD_REQUEST"
                        }
                    )
                    audioList.add(
                        when (subteam) {
                            "e" -> "electrical.m4a"
                            "m" -> "mechanical.m4a"
                            "p" -> "programming.m4a"
                            "a" -> "all_hands.m4a"
                            else -> "BAD_REQUEST"
                        }
                    )
                    audioList.add("issue_with_the.m4a")
                    audioList.add(
                        when (subsystem) {
                            "ind" -> "indexer.m4a"
                            "shtr" -> "launcher.m4a"
                            "clmb" -> "climb.m4a"
                            "dt" -> "drivetrain.m4a"
                            "int" -> "intake.m4a"
                            "hand" -> "handoff.m4a"
                            else -> "BAD_REQUEST"
                        }
                    )
                    i += 3
                }
                for (audio in audioList) {
                    Log.d(TAG, "Audio to play: $audio")
                }
                if ("BAD_REQUEST" !in audioList) {
                    soundAlarmManager.playMessage(audioList)
                    soundAlarmManager.playTTSVoice()
                } else{
                    Log.e(TAG, "Received message with invalid format: $messageBody")
                    soundAlarmManager.playMessage(listOf("intro.m4a", "sev_1.m4a", "there_is_a.m4a"))
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
