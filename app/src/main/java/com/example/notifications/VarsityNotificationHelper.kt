package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.NotificationType

object VarsityNotificationHelper {
    const val CHANNEL_CHAT_MESSAGES = "varsitybw_chat_messages"
    const val CHANNEL_POST_REPLIES = "varsitybw_post_replies"
    const val CHANNEL_SOCIAL_ALERTS = "varsitybw_social_alerts"

    const val EXTRA_NOTIF_TYPE = "extra_notif_type"
    const val EXTRA_TARGET_ID = "extra_target_id"
    const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val chatChannel = NotificationChannel(
            CHANNEL_CHAT_MESSAGES,
            "Campus Chats & DMs",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when you receive new direct or study group chat messages."
        }

        val repliesChannel = NotificationChannel(
            CHANNEL_POST_REPLIES,
            "Feed Replies",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when students reply to your Campus Feed posts."
        }

        val defaultChannel = NotificationChannel(
            CHANNEL_SOCIAL_ALERTS,
            "VarsityBW Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General VarsityBW push alerts and campus updates."
        }

        manager.createNotificationChannels(listOf(chatChannel, repliesChannel, defaultChannel))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showSocialNotification(
        context: Context,
        notifId: String,
        type: NotificationType,
        title: String,
        body: String,
        targetId: String
    ) {
        if (!hasNotificationPermission(context)) return
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        ensureChannels(context)

        val channelId = when (type) {
            NotificationType.CHAT_MESSAGE -> CHANNEL_CHAT_MESSAGES
            NotificationType.POST_REPLY -> CHANNEL_POST_REPLIES
            NotificationType.ADMIN_REPORT -> CHANNEL_SOCIAL_ALERTS
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIF_TYPE, type.name)
            putExtra(EXTRA_TARGET_ID, targetId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }

        val requestCode = (notifId.hashCode() and 0x7fffffff)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "VarsityBW" })
            .setContentText(body.ifBlank { "New campus activity" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            nm.notify(requestCode, notification)
        } catch (_: SecurityException) {
            // Permission revoked mid-dispatch
        }
    }
}
