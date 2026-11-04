package com.example.telephony

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log

class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val action = intent.action
        if (TelephonyManager.ACTION_RESPOND_VIA_MESSAGE == action) {
            val extras = intent.extras
            if (extras != null) {
                val message = extras.getString(Intent.EXTRA_TEXT) ?: ""
                val uri: Uri? = intent.data
                val recipient = uri?.schemeSpecificPart ?: ""
                if (recipient.isNotBlank() && message.isNotBlank()) {
                    Log.d(TAG, "Headless quick reply sending to $recipient: $message")
                    SmsSenderHelper.sendTextMessage(this, recipient, message)
                }
            }
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }

    companion object {
        private const val TAG = "KNKTHeadlessSms"
    }
}
