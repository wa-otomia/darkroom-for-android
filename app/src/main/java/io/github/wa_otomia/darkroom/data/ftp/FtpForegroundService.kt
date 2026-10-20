package io.github.wa_otomia.darkroom.data.ftp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.wa_otomia.darkroom.MainActivity
import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.FgsService
import io.github.wa_otomia.darkroom.core.FgsStartOrigin
import io.github.wa_otomia.darkroom.core.FgsStopPlan
import io.github.wa_otomia.darkroom.core.FgsStopStep
import io.github.wa_otomia.darkroom.core.FtpAutoStartSkip
import io.github.wa_otomia.darkroom.core.fgsRejectedPlan
import io.github.wa_otomia.darkroom.core.fgsStartAllowed
import io.github.wa_otomia.darkroom.core.fgsTimeoutPlan
import io.github.wa_otomia.darkroom.core.ftpAutoStartSkip
import io.github.wa_otomia.darkroom.core.hasLocalNetworkPermission
import io.github.wa_otomia.darkroom.data.automation.Automation
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.FgsPauseCause
import io.github.wa_otomia.darkroom.data.settings.ForegroundPause
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.data.settings.foregroundPause
import io.github.wa_otomia.darkroom.data.settings.isForegroundStartNotAllowed
import io.github.wa_otomia.darkroom.data.transfer.TransferRegistry
import io.github.wa_otomia.darkroom.data.transfer.TransferState
import io.github.wa_otomia.darkroom.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class FtpForegroundService : Service() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var catalog: CatalogRepository
    @Inject lateinit var automation: Automation
    @Inject lateinit var activityLog: ActivityLog
    @Inject lateinit var registry: TransferRegistry
    @Inject lateinit var foregroundPause: ForegroundPause

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var server: CameraFtpServer? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var inForeground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfServer()
            removeForeground()
            stopSelf()
            listening.value = false
            localNetworkBlocked.value = false
            return START_NOT_STICKY
        }
        // Null intent is a sticky restart. It has no foreground obligation while paused.
        if (intent == null && !fgsStartAllowed(FgsStartOrigin.StickyRestart, isPaused())) {
            logStickyRestartSkipped()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!enterForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_RESTART -> {
                stopSelfServer()
                if (!startServer()) return START_NOT_STICKY
            }
            else -> if (!startServer()) return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Read listening before stop. The plan does not join sessions or ingest.
        applyPlan(fgsTimeoutPlan(FgsService.Ftp, server != null), FgsPauseCause.Timeout)
    }

    private fun isPaused(): Boolean = foregroundPause.current() != null

    /** Returns false when the system rejected foreground start and the plan has stopped this instance. */
    private fun enterForeground(): Boolean {
        if (inForeground) return true
        return try {
            startFtpForeground(notification(getString(R.string.ftp_starting)))
            inForeground = true
            true
        } catch (e: Exception) {
            if (!e.isForegroundStartNotAllowed()) throw e
            applyPlan(fgsRejectedPlan(FgsService.Ftp, inService = true), FgsPauseCause.StartRejected)
            false
        }
    }

    private fun applyPlan(plan: FgsStopPlan, cause: FgsPauseCause) {
        for (step in plan.steps) {
            when (step) {
                FgsStopStep.StopListening -> stopSelfServer()
                FgsStopStep.RemoveForeground -> removeForeground()
                FgsStopStep.StopSelf -> stopSelf()
                FgsStopStep.RecordPause -> foregroundPause.record(
                    FgsService.Ftp,
                    cause,
                    plan.ftpWasListening,
                )
                FgsStopStep.NotifyFtpPaused -> FtpPausedNotification.show(this)
            }
        }
    }

    private fun logStickyRestartSkipped() {
        try {
            activityLog.record("ftp", "sticky restart skipped: paused by system time limit")
        } catch (_: Exception) {
            // Stopping still has to happen. A log failure must not keep this instance up.
        }
    }

    private fun startServer(): Boolean {
        if (server != null) {
            FtpPausedNotification.cancel(this)
            return true
        }
        if (!hasLocalNetworkPermission(this)) {
            activityLog.record("ftp", "local network permission missing", "error")
            localNetworkBlocked.value = true
            listening.value = false
            removeForeground()
            stopSelf()
            return false
        }
        localNetworkBlocked.value = false
        val cfg = settings.readSettings()
        val inbox = catalog.inboxDir()
        val wifi = applicationContext.getSystemService(WifiManager::class.java)
        wifiLock = wifi?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "darkroom-ftp")?.also { lock ->
            try {
                lock.acquire()
            } catch (e: SecurityException) {
                activityLog.record("ftp", "wifi lock denied", "error", e.message)
                wifiLock = null
            }
        }
        val hooks = object : FtpHooks {
            override fun sessionOpened(id: String, remote: String) = registry.sessionOpened(id, remote)
            override fun sessionAuthed(id: String, user: String) = registry.sessionAuthed(id, user)
            override fun sessionClosed(id: String) = registry.sessionClosed(id)
            override fun transferStarted(sessionId: String, filename: String, total: Long?): String =
                registry.transferStarted(sessionId, filename, total).id
            override fun ingestStarted(sessionId: String, filename: String, bytes: Long): String =
                registry.transferStarted(
                    sessionId,
                    filename,
                    total = bytes.takeIf { it > 0 },
                    bytes = bytes,
                    state = TransferState.Ingesting,
                ).id
            override fun sessionActivity(sessionId: String, filename: String, bytes: Long) =
                registry.sessionActivity(sessionId, filename, bytes)
            override fun transferProgress(transferId: String, bytes: Long, total: Long?) =
                registry.transferProgress(transferId, bytes, total)
            override fun transferFinished(transferId: String) = registry.receiveFinished(transferId)
            override fun transferFailed(transferId: String, error: String) = registry.failed(transferId, error)
        }
        val srv = CameraFtpServer(
            bind = "0.0.0.0",
            port = cfg.ftpPort,
            user = cfg.ftpUser,
            password = cfg.ftpPassword,
            pasvMin = cfg.ftpPasvMin,
            pasvMax = cfg.ftpPasvMax,
            inbox = inbox,
            hooks = hooks,
            onUploaded = { file, transferId ->
                appScope.launch(Dispatchers.IO) {
                    try {
                        if (registry.isInterrupted(transferId)) {
                            file.delete()
                            return@launch
                        }
                        val photoId = registry.transfers.value.find { it.id == transferId }?.photoId
                            ?: UUID.randomUUID().toString()
                        registry.receiveFinished(transferId, file.length())
                        val photo = catalog.ingestFile(
                            file,
                            "ftp",
                            id = photoId,
                            onPhase = { phase ->
                                when (phase) {
                                    "transcoding" -> registry.transcoding(transferId)
                                    "ingesting" -> registry.ingesting(transferId)
                                }
                            },
                        )
                        registry.done(transferId)
                        runCatching { automation.onIngested(photo.id, "ftp") }
                            .onFailure { activityLog.record("automation", "onIngested failed", "error", it.message) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        file.delete()
                        registry.failed(transferId, e.message ?: e.toString())
                        activityLog.record("ftp", "ingest failed", "error", e.message)
                    }
                }
            },
            log = { activityLog.record("ftp", it) },
        )
        try {
            srv.start()
            server = srv
            listening.value = true
            listenPort.value = cfg.ftpPort
            listenUser.value = cfg.ftpUser
            listenPassword.value = cfg.ftpPassword
            listenPasvMin.value = cfg.ftpPasvMin
            listenPasvMax.value = cfg.ftpPasvMax
            FtpPausedNotification.cancel(this)
            val ip = CameraFtpServer.wlanHostOneLine().ifBlank { "0.0.0.0" }
            updateFtpNotification(notification(getString(R.string.ftp_listening_fmt, ip, cfg.ftpPort)))
            return true
        } catch (e: Exception) {
            activityLog.record("ftp", "listen failed", "error", e.message)
            listening.value = false
            stopSelf()
            return false
        }
    }

    private fun stopSelfServer() {
        try {
            server?.stop()
        } catch (_: Exception) {
        }
        server = null
        registry.interruptReceiving()
        if (wifiLock?.isHeld == true) wifiLock?.release()
        wifiLock = null
        listening.value = false
    }

    override fun onDestroy() {
        stopSelfServer()
        super.onDestroy()
    }

    private fun createChannel() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.ftp_channel), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun startFtpForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun removeForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        inForeground = false
    }

    private fun updateFtpNotification(notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, notification)
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, FtpForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_safelight)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, getString(R.string.ftp_stop), stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "io.github.wa_otomia.darkroom.FTP_STOP"
        const val ACTION_RESTART = "io.github.wa_otomia.darkroom.FTP_RESTART"
        const val CHANNEL = "ftp"
        const val NOTIF_ID = 21
        val listening = MutableStateFlow(false)
        val localNetworkBlocked = MutableStateFlow(false)
        val listenPort = MutableStateFlow(2121)
        val listenUser = MutableStateFlow("camera")
        val listenPassword = MutableStateFlow("")
        val listenPasvMin = MutableStateFlow(50000)
        val listenPasvMax = MutableStateFlow(50010)

        fun start(context: Context) {
            launchService(context, Intent(context, FtpForegroundService::class.java))
        }

        /** Same permission gates as the Transfer-screen start action, without UI prompts. */
        fun startIfPermitted(context: Context, activityLog: ActivityLog): Boolean {
            when (autoStartSkip(context)) {
                FtpAutoStartSkip.LocalNetworkPermission -> {
                    activityLog.record("ftp", "auto-start skipped: local network permission missing")
                    return false
                }
                FtpAutoStartSkip.NotificationPermission -> {
                    activityLog.record("ftp", "auto-start skipped: notifications permission missing")
                    return false
                }
                null -> Unit
            }
            return try {
                start(context)
                true
            } catch (e: Exception) {
                activityLog.record("ftp", "auto-start failed", "error", e.message)
                false
            }
        }

        fun autoStartSkip(context: Context): FtpAutoStartSkip? {
            val notificationsGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            return ftpAutoStartSkip(hasLocalNetworkPermission(context), notificationsGranted)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FtpForegroundService::class.java).setAction(ACTION_STOP))
        }

        fun restart(context: Context) {
            launchService(
                context,
                Intent(context, FtpForegroundService::class.java).setAction(ACTION_RESTART),
            )
        }

        private fun launchService(context: Context, intent: Intent) {
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                if (!e.isForegroundStartNotAllowed()) throw e
                degradeRejectedStart(context)
            }
        }

        private fun degradeRejectedStart(context: Context) {
            val plan = fgsRejectedPlan(FgsService.Ftp, inService = false)
            for (step in plan.steps) {
                when (step) {
                    FgsStopStep.RecordPause -> foregroundPause(context).record(
                        FgsService.Ftp,
                        FgsPauseCause.StartRejected,
                        plan.ftpWasListening,
                    )
                    FgsStopStep.NotifyFtpPaused -> FtpPausedNotification.show(context)
                    FgsStopStep.StopListening, FgsStopStep.RemoveForeground, FgsStopStep.StopSelf -> Unit
                }
            }
        }
    }
}
