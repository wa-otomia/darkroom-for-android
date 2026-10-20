package io.github.wa_otomia.darkroom.data.jobs

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
import io.github.wa_otomia.darkroom.MainActivity
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.FgsService
import io.github.wa_otomia.darkroom.core.FgsStopPlan
import io.github.wa_otomia.darkroom.core.FgsStopStep
import io.github.wa_otomia.darkroom.core.fgsRejectedPlan
import io.github.wa_otomia.darkroom.core.fgsTimeoutPlan
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.FgsPauseCause
import io.github.wa_otomia.darkroom.data.settings.ForegroundPause
import io.github.wa_otomia.darkroom.data.settings.isForegroundStartNotAllowed
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
    @Inject lateinit var jobsController: JobsServiceController
    @Inject lateinit var foregroundPause: ForegroundPause

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var foregroundStartRejected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        try {
            startJobsForeground(notification(0, 0))
        } catch (e: Exception) {
            if (!e.isForegroundStartNotAllowed()) throw e
            foregroundStartRejected = true
            try {
                applyPlan(fgsRejectedPlan(FgsService.Jobs, inService = true), FgsPauseCause.StartRejected)
            } finally {
                jobsController.onServiceStopped()
            }
            return
        }
        scope.launch {
            combine(printQueue.printJobs, aiJobs.jobs) { prints, ais ->
                prints.count { it.state == PrintQueue.STATE_QUEUED || it.state == PrintQueue.STATE_RUNNING } to ais.size
            }.collect { (printCount, aiCount) ->
                updateJobsNotification(notification(printCount, aiCount))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (foregroundStartRejected) return START_NOT_STICKY
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // This scope only refreshes the notification. Print and AI jobs stay on the application scope.
        scope.cancel()
        try {
            applyPlan(fgsTimeoutPlan(FgsService.Jobs), FgsPauseCause.Timeout)
        } finally {
            jobsController.onServiceStopped()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun applyPlan(plan: FgsStopPlan, cause: FgsPauseCause) {
        for (step in plan.steps) {
            when (step) {
                FgsStopStep.RemoveForeground -> stopForeground(STOP_FOREGROUND_REMOVE)
                FgsStopStep.StopSelf -> stopSelf()
                FgsStopStep.RecordPause -> foregroundPause.record(
                    FgsService.Jobs,
                    cause,
                    plan.ftpWasListening,
                )
                FgsStopStep.StopListening, FgsStopStep.NotifyFtpPaused -> Unit
            }
        }
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
        const val ACTION_STOP = "io.github.wa_otomia.darkroom.JOBS_STOP"
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
