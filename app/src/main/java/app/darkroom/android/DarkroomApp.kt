package app.darkroom.android

import android.app.Application
import app.darkroom.android.data.ftp.FtpForegroundService
import app.darkroom.android.data.jobs.JobsServiceController
import app.darkroom.android.data.jobs.KeepAliveService
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
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
