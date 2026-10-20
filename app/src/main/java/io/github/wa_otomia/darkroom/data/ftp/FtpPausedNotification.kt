package io.github.wa_otomia.darkroom.data.ftp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.github.wa_otomia.darkroom.MainActivity
import io.github.wa_otomia.darkroom.R

/**
 * Default-importance notice that FTP was paused by the system time limit.
 * Tapping opens [MainActivity] with no action and no extras. Request code 2
 * stays clear of the running notification's 0 and 1.
 */
object FtpPausedNotification {
    const val CHANNEL = "ftp_paused"
    const val NOTIF_ID = 24

    fun show(context: Context) {
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL) == null) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.ftp_paused_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_safelight)
            .setContentTitle(context.getString(R.string.ftp_paused_title))
            .setContentText(context.getString(R.string.ftp_paused_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        mgr.notify(NOTIF_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID)
    }
}
