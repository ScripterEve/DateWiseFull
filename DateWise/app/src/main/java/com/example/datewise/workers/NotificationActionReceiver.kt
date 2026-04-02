package com.example.datewise.workers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.datewise.data.DateWiseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.datewise.ACTION_DELETE_PRODUCT") {
            val productId = intent.getIntExtra("PRODUCT_ID", -1)
            
            if (productId != -1) {
                // Launch a quick coroutine to delete from the database
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = DateWiseDatabase.getDatabase(context)
                        database.productDao().deleteById(productId)
                        
                        // Cancel the notification visually
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(productId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (intent.action == "com.example.datewise.ACTION_DISMISS_PRODUCT") {
            val productId = intent.getIntExtra("PRODUCT_ID", -1)
            if (productId != -1) {
                // Just cancel the notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(productId)
            }
        }
    }
}
