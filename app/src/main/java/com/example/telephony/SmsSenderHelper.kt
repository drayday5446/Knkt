package com.example.telephony

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

object SmsSenderHelper {
    private const val TAG = "KNKTSmsSender"

    fun sendTextMessage(context: Context, destinationAddress: String, text: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            if (text.length > 160) {
                val parts = smsManager.divideMessage(text)
                smsManager.sendMultipartTextMessage(destinationAddress, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destinationAddress, null, text, null, null)
            }
            Log.d(TAG, "SMS successfully dispatched to $destinationAddress")
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SEND_SMS permission not granted or device restricted: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $destinationAddress: ${e.message}")
            false
        }
    }
}
