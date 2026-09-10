package io.github.wa_otomia.darkroom

import android.app.Application
import io.github.wa_otomia.darkroom.data.ftp.FtpForegroundService
import io.github.wa_otomia.darkroom.data.jobs.JobsServiceController
import io.github.wa_otomia.darkroom.data.jobs.KeepAliveService
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DarkroomApp : Application() {
    @Inject lateinit var jobsController: JobsServiceController
    @Inject lateinit var printQueue: PrintQueue
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var activityLog: ActivityLog

    override fun onCreate() {
        super.onCreate()
        jobsController.start()
        printQueue.restore()
        val cfg = settings.readSettings()
        if (cfg.keepAlive) {
            runCatching { KeepAliveService.start(this) }
                .onFailure { activityLog.record("keepalive", "start failed", "error", it.message) }
        }
        if (cfg.ftpAutoStart) FtpForegroundService.startIfPermitted(this, activityLog)
    }
}
