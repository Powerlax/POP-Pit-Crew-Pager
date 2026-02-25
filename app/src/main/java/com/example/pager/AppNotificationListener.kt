package com.example.pager

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AppNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "AppNotificationListener"
        private const val GOOGLE_MESSAGES_PACKAGE = "com.google.android.apps.messaging"
        private const val BACKGROUND_WORK_TEXT = "Messages is doing work in the background"
        var onMessageReceived: ((sender: String, messageBody: String) -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != GOOGLE_MESSAGES_PACKAGE) {
            return
        }
        val extras = sbn.notification.extras
        val (sender, messageBody) = extractMessage(extras)
        if (messageBody.isNullOrBlank()) {
            Log.d(TAG, "Google Messages notification with no text")
            return
        }
        if (messageBody == BACKGROUND_WORK_TEXT) {
            Log.d(TAG, "Ignoring background work notification")
            return
        }
        val safeSender = sender ?: "Unknown"
        Log.d(TAG, "Notification from $safeSender: $messageBody")
        onMessageReceived?.invoke(safeSender, messageBody)
    }

    private fun extractMessage(extras: Bundle): Pair<String?, String?> {
        val messaging = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            ?.mapNotNull { it as? Bundle }
            ?.mapNotNull { bundle ->
                val text = bundle.getCharSequence("text")?.toString()
                val sender = bundle.getCharSequence("sender")?.toString()
                if (text.isNullOrBlank()) null else sender to text
            }
        val lastMessage = messaging?.lastOrNull()
        if (lastMessage != null) {
            return lastMessage
        }
        val title = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        return title to text
    }
}
