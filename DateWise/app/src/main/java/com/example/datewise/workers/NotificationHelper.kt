package com.example.datewise.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.datewise.MainActivity
import com.example.datewise.R
import com.example.datewise.data.Product

object NotificationHelper {

    private const val CHANNEL_ID = "expiry_alerts_channel"
    private const val CHANNEL_NAME = "Expiry Alerts"
    private const val CHANNEL_DESC = "Notifications for products that are about to expire"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendExpiryNotification(context: Context, product: Product, daysLeft: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open the app on tap
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            product.id, // Use product ID to uniquely identify the intent
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Delete
        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.datewise.ACTION_DELETE_PRODUCT"
            putExtra("PRODUCT_ID", product.id)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            product.id,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Dismiss
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.datewise.ACTION_DISMISS_PRODUCT"
            putExtra("PRODUCT_ID", product.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            product.id + 10000, // Different request code so it doesn't collide with delete
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (daysLeft) {
            1L -> "⚠️ Expires Tomorrow!"
            else -> "⏰ Expires in $daysLeft days"
        }

        val text = "${product.name} is expiring soon."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Fallback icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent) // Dispatches to receiver which clears the notification
            .addAction(0, "Delete", deletePendingIntent)

        // Give each notification a unique ID so they don't overwrite each other
        notificationManager.notify(product.id, builder.build())
    }
}
