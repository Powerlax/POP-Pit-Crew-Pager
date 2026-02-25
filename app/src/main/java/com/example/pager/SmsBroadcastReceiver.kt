package com.example.pager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
        var onMessageReceived: ((sender: String, messageBody: String) -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        for (smsMessage in messages) {
            val sender = smsMessage.displayOriginatingAddress
            val messageBody = smsMessage.messageBody

            Log.d(TAG, "SMS received from: $sender")
            Log.d(TAG, "Message body: $messageBody")

            // Trigger the callback if set
            onMessageReceived?.invoke(sender, messageBody)

            // You can add filtering logic here later
            // For example: if (sender.contains("group-chat-identifier")) { ... }
        }
    }
}

