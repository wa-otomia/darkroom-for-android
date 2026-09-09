package app.darkroom.android.data.ftp

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
import app.darkroom.android.MainActivity
import app.darkroom.android.R
import app.darkroom.android.core.hasLocalNetworkPermission
import app.darkroom.android.data.automation.Automation
import app.darkroom.android.data.catalog.CatalogRepository
import app.darkroom.android.data.settings.ActivityLog
import app.darkroom.android.data.settings.SettingsRepository
import app.darkroom.android.data.transfer.TransferRegistry
import app.darkroom.android.data.transfer.TransferState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: CameraFtpServer? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startFtpForeground(notification(getString(R.string.ftp_starting)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelfServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                listening.value = false
                localNetworkBlocked.value = false
                return START_NOT_STICKY
            }
            ACTION_RESTART -> {
                stopSelfServer()
                if (!startServer()) return START_NOT_STICKY
            }
            else -> if (!startServer()) return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startServer(): Boolean {
        if (server != null) return true
        if (!hasLocalNetworkPermission(this)) {
            activityLog.record("ftp", "local network permission missing", "error")
            localNetworkBlocked.value = true
            listening.value = false
            stopForeground(STOP_FOREGROUND_REMOVE)
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
                scope.launch {
                    try {
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
        if (wifiLock?.isHeld == true) wifiLock?.release()
        wifiLock = null
        listening.value = false
    }

    override fun onDestroy() {
        stopSelfServer()
        scope.cancel()
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
        const val ACTION_STOP = "app.darkroom.android.FTP_STOP"
        const val ACTION_RESTART = "app.darkroom.android.FTP_RESTART"
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
            context.startForegroundService(Intent(context, FtpForegroundService::class.java))
        }

        /** Same permission gates as the Transfer-screen start action, without UI prompts. */
        fun startIfPermitted(context: Context, activityLog: ActivityLog): Boolean {
            if (!hasLocalNetworkPermission(context)) {
                activityLog.record("ftp", "auto-start skipped: local network permission missing")
                return false
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                activityLog.record("ftp", "auto-start skipped: notifications permission missing")
                return false
            }
            return try {
                start(context)
                true
            } catch (e: Exception) {
                activityLog.record("ftp", "auto-start failed", "error", e.message)
                false
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FtpForegroundService::class.java).setAction(ACTION_STOP))
        }

        fun restart(context: Context) {
            context.startForegroundService(Intent(context, FtpForegroundService::class.java).setAction(ACTION_RESTART))
        }
    }
}
