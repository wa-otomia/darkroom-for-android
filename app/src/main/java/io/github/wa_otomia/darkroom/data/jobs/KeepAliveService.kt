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
import io.github.wa_otomia.darkroom.core.FgsStartOrigin
import io.github.wa_otomia.darkroom.core.FgsStopPlan
import io.github.wa_otomia.darkroom.core.FgsStopStep
import io.github.wa_otomia.darkroom.core.fgsRejectedPlan
import io.github.wa_otomia.darkroom.core.fgsStartAllowed
import io.github.wa_otomia.darkroom.core.fgsTimeoutPlan
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.FgsPauseCause
import io.github.wa_otomia.darkroom.data.settings.foregroundPause
import io.github.wa_otomia.darkroom.data.settings.isForegroundStartNotAllowed
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class KeepAliveService : Service() {
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            inForeground = false
            stopSelf()
            return START_NOT_STICKY
        }
        // Null intent is a sticky restart. It has no foreground obligation while paused.
        if (intent == null && !fgsStartAllowed(FgsStartOrigin.StickyRestart, isPaused())) {
            logStickyRestartSkipped()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!enterForeground()) return START_NOT_STICKY
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Plan order stops the service before the pause write. The saved switch is not changed.
        applyPlan(fgsTimeoutPlan(FgsService.KeepAlive), FgsPauseCause.Timeout)
    }

    private fun isPaused(): Boolean = foregroundPause(this).current() != null

    /** Returns false when the system rejected foreground start and the plan has stopped this instance. */
    private fun enterForeground(): Boolean {
        if (inForeground) return true
        return try {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
            inForeground = true
            true
        } catch (e: Exception) {
            if (!e.isForegroundStartNotAllowed()) throw e
            applyPlan(fgsRejectedPlan(FgsService.KeepAlive, inService = true), FgsPauseCause.StartRejected)
            false
        }
    }

    private fun applyPlan(plan: FgsStopPlan, cause: FgsPauseCause) {
        for (step in plan.steps) {
            when (step) {
                FgsStopStep.RemoveForeground -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    inForeground = false
                }
                FgsStopStep.StopSelf -> stopSelf()
                FgsStopStep.RecordPause -> foregroundPause(this).record(
                    FgsService.KeepAlive,
                    cause,
                    plan.ftpWasListening,
                )
                FgsStopStep.StopListening, FgsStopStep.NotifyFtpPaused -> Unit
            }
        }
    }

    private fun logStickyRestartSkipped() {
        try {
            activityLog(this).record("keepalive", "sticky restart skipped: paused by system time limit")
        } catch (_: Exception) {
            // Stopping still has to happen. A log failure must not keep this instance up.
        }
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.keep_alive_channel), NotificationManager.IMPORTANCE_MIN),
        )
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_safelight)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.keep_alive_notification))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val ACTION_STOP = "io.github.wa_otomia.darkroom.KEEP_ALIVE_STOP"
        const val CHANNEL = "keep_alive"
        const val NOTIF_ID = 23

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, KeepAliveService::class.java))
            } catch (e: Exception) {
                if (!e.isForegroundStartNotAllowed()) throw e
                val plan = fgsRejectedPlan(FgsService.KeepAlive, inService = false)
                for (step in plan.steps) {
                    if (step == FgsStopStep.RecordPause) {
                        foregroundPause(context).record(
                            FgsService.KeepAlive,
                            FgsPauseCause.StartRejected,
                            plan.ftpWasListening,
                        )
                    }
                }
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, KeepAliveService::class.java).setAction(ACTION_STOP))
        }

        fun apply(context: Context, enabled: Boolean) {
            if (enabled) start(context) else stop(context)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface KeepAliveActivityLogEntryPoint {
    fun activityLog(): ActivityLog
}

private fun activityLog(context: Context): ActivityLog =
    EntryPointAccessors.fromApplication(context, KeepAliveActivityLogEntryPoint::class.java)
        .activityLog()
