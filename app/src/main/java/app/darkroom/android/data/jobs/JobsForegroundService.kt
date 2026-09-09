package app.darkroom.android.data.jobs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.darkroom.android.MainActivity
import app.darkroom.android.R
import app.darkroom.android.data.printer.PrintQueue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JobsForegroundService : Service() {
    @Inject lateinit var printQueue: PrintQueue
    @Inject lateinit var aiJobs: AiJobs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startJobsForeground(notification(0, 0))
        scope.launch {
            combine(printQueue.printJobs, aiJobs.jobs) { prints, ais ->
                prints.count { it.state == PrintQueue.STATE_QUEUED || it.state == PrintQueue.STATE_RUNNING } to ais.size
            }.collect { (printCount, aiCount) ->
                updateJobsNotification(notification(printCount, aiCount))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startJobsForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun updateJobsNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification)
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, "后台任务", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification(printCount: Int, aiCount: Int): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_safelight)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(jobsNotificationText(printCount, aiCount))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "app.darkroom.android.JOBS_STOP"
        const val CHANNEL = "jobs"
        const val NOTIF_ID = 22

        fun start(context: Context) {
            context.startForegroundService(Intent(context, JobsForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, JobsForegroundService::class.java).setAction(ACTION_STOP))
        }
    }
}

internal fun jobsNotificationText(printCount: Int, aiCount: Int): String = when {
    printCount > 0 && aiCount > 0 -> "打印队列 ${printCount} 项 · AI 处理中 ${aiCount} 项"
    printCount > 0 -> "打印队列 ${printCount} 项 · 正在打印…"
    aiCount > 0 -> "AI 处理中 ${aiCount} 项"
    else -> "后台任务"
}
