package io.github.wa_otomia.darkroom

import android.app.Application
import io.github.wa_otomia.darkroom.core.FgsStartOrigin
import io.github.wa_otomia.darkroom.core.FtpAutoStartSkip
import io.github.wa_otomia.darkroom.core.fgsResumePlan
import io.github.wa_otomia.darkroom.core.fgsStartAllowed
import io.github.wa_otomia.darkroom.data.ftp.FtpForegroundService
import io.github.wa_otomia.darkroom.data.ftp.FtpPausedNotification
import io.github.wa_otomia.darkroom.data.jobs.JobsServiceController
import io.github.wa_otomia.darkroom.data.jobs.KeepAliveService
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.ForegroundPause
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DarkroomApp : Application() {
    @Inject lateinit var jobsController: JobsServiceController
    @Inject lateinit var printQueue: PrintQueue
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var activityLog: ActivityLog
    @Inject lateinit var foregroundPause: ForegroundPause

    override fun onCreate() {
        super.onCreate()
        jobsController.start()
        printQueue.restore()
        val paused = foregroundPause.current() != null
        if (!fgsStartAllowed(FgsStartOrigin.ProcessAutoStart, paused)) {
            activityLog.record("keepalive", "auto-start skipped: paused by system time limit")
            activityLog.record("ftp", "auto-start skipped: paused by system time limit")
        } else {
            val cfg = settings.readSettings()
            if (cfg.keepAlive) {
                runCatching { KeepAliveService.start(this) }
                    .onFailure { activityLog.record("keepalive", "start failed", "error", it.message) }
            }
            if (cfg.ftpAutoStart) FtpForegroundService.startIfPermitted(this, activityLog)
        }
    }

    /**
     * Visible-resume path. Returns immediately when nothing is paused; otherwise
     * follows [fgsResumePlan]: clear the record, log an FTP skip, cancel the pause
     * notice, start FTP and keep-alive, then reevaluate jobs.
     */
    fun resumeAfterForegroundPause() {
        val pause = foregroundPause.current() ?: return
        val plan = fgsResumePlan(
            pause,
            settings.readSettings().keepAlive,
            FtpForegroundService.autoStartSkip(this),
        )
        if (plan.clearPause) foregroundPause.clear()
        plan.ftpSkip?.let { activityLog.record("ftp", it.skipLog()) }
        if (plan.cancelFtpNotice) FtpPausedNotification.cancel(this)
        if (plan.startFtp) {
            runCatching { FtpForegroundService.start(this) }
                .onFailure { activityLog.record("ftp", "start failed", "error", it.message) }
        }
        if (plan.startKeepAlive) {
            runCatching { KeepAliveService.start(this) }
                .onFailure { activityLog.record("keepalive", "start failed", "error", it.message) }
        }
        if (plan.reevaluateJobs) jobsController.reevaluate()
    }
}

private fun FtpAutoStartSkip.skipLog(): String = when (this) {
    FtpAutoStartSkip.LocalNetworkPermission -> "auto-start skipped: local network permission missing"
    FtpAutoStartSkip.NotificationPermission -> "auto-start skipped: notifications permission missing"
}
