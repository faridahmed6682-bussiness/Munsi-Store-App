package com.munsistore.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.munsistore.app.R

/** Local (on-device) order notifications. No server push — the app posts a
 *  notification itself whenever an order's status changes on this device. */
object NotificationHelper {
    private const val CHANNEL_ID = "order_updates"
    private const val CHANNEL_NAME = "Order updates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            // Notifications are best-effort; never crash the order flow.
        }
    }

    fun notifyOrderStatus(context: Context, orderId: String, statusText: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }
            ensureChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("$orderId: $statusText")
                .setAutoCancel(true)
                .build()
            manager.notify(orderId.hashCode(), notification)
        } catch (e: Exception) {
            // Best-effort only.
        }
    }
}
