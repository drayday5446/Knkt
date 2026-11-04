package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        if (intent.action == "android.provider.Telephony.WAP_PUSH_DELIVERED") {
            Log.d(TAG, "MMS WAP PUSH DELIVERED received")
            // Android OS requires this receiver to be registered with application/vnd.wap.mms-message
            // to permit becoming the default SMS app.
        }
    }

    companion object {
        private const val TAG = "KNKTMmsReceiver"
    }
}
