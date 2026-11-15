package com.movepulse.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
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
import com.movepulse.MainActivity

object Notifier {
    const val SERVICE_ID = 1
    private const val PROGRESS_ID = 100
    private const val REMINDER_ID = 200
    private const val CH_SERVICE = "service"
    private const val CH_PROGRESS = "progress"
    private const val CH_REMINDER = "reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Tracking", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "Progress & motivation", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Move reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun serviceNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CH_SERVICE)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("MovePulse is tracking your movement")
            .setContentText("Keep moving, every step counts!")
            .setOngoing(true)
            .setContentIntent(openApp(context))
            .build()

    fun progress(context: Context, title: String, text: String) =
        post(context, CH_PROGRESS, PROGRESS_ID, title, text)

    fun reminder(context: Context, title: String, text: String) =
        post(context, CH_REMINDER, REMINDER_ID, title, text)

    @SuppressLint("MissingPermission")
    private fun post(context: Context, channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}
