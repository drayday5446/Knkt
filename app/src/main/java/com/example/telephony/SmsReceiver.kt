package com.example.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != "android.provider.Telephony.SMS_DELIVERED" &&
            action != "android.provider.Telephony.SMS_RECEIVED"
        ) {
            return
        }

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing SMS intent: ${e.message}")
            return
        }

        if (messages.isNullOrEmpty()) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val pendingResult = goAsync()

        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context, scope)
                val convDao = db.conversationDao()
                val msgDao = db.messageDao()

                // Group by originating address in case of multi-part messages
                val messagesBySender = messages.groupBy { it.displayOriginatingAddress ?: it.originatingAddress ?: "Unknown" }

                for ((senderAddress, smsList) in messagesBySender) {
                    val fullBody = smsList.joinToString(separator = "") { it.displayMessageBody ?: it.messageBody ?: "" }
                    val timestamp = smsList.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()

                    var conv = convDao.getConversationByPhone(senderAddress)
                    val convId = if (conv != null) {
                        if (conv.isTrash) {
                            convDao.restoreFromTrash(conv.id)
                        }
                        convDao.updateLastMessage(
                            id = conv.id,
                            lastMsg = fullBody,
                            timestamp = timestamp,
                            protocol = "SMS",
                            isEncrypted = false
                        )
                        conv.id
                    } else {
                        val newConv = ConversationEntity(
                            recipientName = senderAddress,
                            recipientPhone = senderAddress,
                            avatarColorHex = 0xFF06B6D4,
                            activeProtocol = "SMS",
                            isE2EE = false,
                            isGroup = false,
                            lastMessage = fullBody,
                            lastTimestamp = timestamp
                        )
                        convDao.insertConversation(newConv)
                    }

                    val incomingMsg = MessageEntity(
                        conversationId = convId,
                        sender = senderAddress,
                        isFromUser = false,
                        text = fullBody,
                        timestamp = timestamp,
                        status = "DELIVERED",
                        protocol = "SMS",
                        isEncrypted = false
                    )
                    msgDao.insertMessage(incomingMsg)
                    Log.d(TAG, "Stored received SMS from $senderAddress in conversation $convId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist incoming SMS: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "KNKTSmsReceiver"
    }
}
