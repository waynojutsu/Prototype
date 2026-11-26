package com.example.notifications

import android.util.Log
import com.example.R
import com.example.data.VarsityRepository
import com.example.model.NotificationType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VarsityMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM registration token refreshed")
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && token.isNotBlank()) {
            serviceScope.launch {
                val db = FirebaseFirestore.getInstance(
                    applicationContext.getString(R.string.firestore_database_id)
                )
                VarsityRepository(db, Firebase.auth).registerFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val rawType = data["type"] ?: data["notifType"]
        val type = NotificationType.fromRaw(rawType)
        val targetId = data["targetId"]
            ?: data["channelId"]
            ?: data["postId"]
            ?: ""
        val title = data["title"]
            ?: message.notification?.title
            ?: when (type) {
                NotificationType.CHAT_MESSAGE -> "New Campus Message"
                NotificationType.POST_REPLY -> "New Reply to Your Post"
                NotificationType.ADMIN_REPORT -> "Flagged Post Moderation Alert"
            }
        val body = data["body"]
            ?: message.notification?.body
            ?: "Open VarsityBW to view the conversation."
        val notifId = data["notifId"]
            ?: message.messageId
            ?: System.currentTimeMillis().toString()

        VarsityNotificationHelper.showSocialNotification(
            context = applicationContext,
            notifId = notifId,
            type = type,
            title = title,
            body = body,
            targetId = targetId
        )
    }

    private companion object {
        const val TAG = "VarsityFCM"
    }
}
