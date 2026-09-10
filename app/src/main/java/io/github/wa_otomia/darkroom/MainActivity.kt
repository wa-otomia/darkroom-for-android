package io.github.wa_otomia.darkroom

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import io.github.wa_otomia.darkroom.data.automation.Automation
import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.core.AiProvider
import io.github.wa_otomia.darkroom.data.ai.AiImageClient
import io.github.wa_otomia.darkroom.data.jobs.AiJobs
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.printer.PrinterBluetooth
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.SettingsRepository
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.transfer.TransferRegistry
import io.github.wa_otomia.darkroom.ui.navigation.DarkroomNav
import io.github.wa_otomia.darkroom.ui.theme.DarkroomTheme
import io.github.wa_otomia.darkroom.ui.theme.Room
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var catalog: CatalogRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var printQueue: PrintQueue
    @Inject lateinit var grok: AiImageClient
    @Inject lateinit var bluetooth: PrinterBluetooth
    @Inject lateinit var activityLog: ActivityLog
    @Inject lateinit var transferRegistry: TransferRegistry
    @Inject lateinit var aiJobs: AiJobs
    @Inject lateinit var automation: Automation

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val openedId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The system bars are configured only here. themes.xml no longer sets
        // statusBarColor / navigationBarColor: those are ignored from API 35 on
        // and, below it, they fought with the scrims enableEdgeToEdge installs.
        //
        // Both bars get a fully transparent scrim so nothing is painted on top
        // of the page behind them, and contrast enforcement is switched off —
        // left on, the platform draws its own translucent band behind the
        // navigation bar, which is the visible seam between the bar area and a
        // full-bleed canvas.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        applyDebugConfig(intent)
        if (savedInstanceState == null) handleShare(intent)
        setContent {
            val open by openedId.collectAsState()
            DarkroomTheme {
                Box(Modifier.fillMaxSize().background(Room)) {
                    DarkroomNav(
                        catalog,
                        settings,
                        printQueue,
                        grok,
                        bluetooth,
                        activityLog,
                        transferRegistry,
                        aiJobs,
                        automation,
                        open,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyDebugConfig(intent)
        handleShare(intent)
    }

    private fun applyDebugConfig(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        var key = intent.getStringExtra("darkroom.grok_key")
        var url = intent.getStringExtra("darkroom.grok_url")
        var openaiKey = intent.getStringExtra("darkroom.openai_key")
        var openaiUrl = intent.getStringExtra("darkroom.openai_url")
        val file = intent.getStringExtra("darkroom.grok_file")
        if (!file.isNullOrBlank()) {
            runCatching {
                java.io.File(file).readLines().forEach { line ->
                    val i = line.indexOf('=')
                    if (i <= 0) return@forEach
                    val k = line.substring(0, i).trim()
                    val v = line.substring(i + 1).trim().trim('"')
                    when (k) {
                        "XAI_API_KEY", "GROK_API_KEY" -> key = v
                        "GROK_MODELS_BASE_URL", "GROK_BASE_URL" -> url = v
                        "OPENAI_API_KEY" -> openaiKey = v
                        "OPENAI_BASE_URL" -> openaiUrl = v
                    }
                }
            }
        }
        val mac = intent.getStringExtra("darkroom.printer_mac")
        val name = intent.getStringExtra("darkroom.printer_name")
        if (!key.isNullOrBlank() || !url.isNullOrBlank()) {
            settings.updateProvider(AiProvider.GROK, key, url, null, null, null)
        }
        if (!openaiKey.isNullOrBlank() || !openaiUrl.isNullOrBlank()) {
            settings.updateProvider(AiProvider.OPENAI, openaiKey, openaiUrl, null, null, null)
        }
        if (!mac.isNullOrBlank()) {
            settings.updateSettings { copy(printerMac = mac, printerName = name.orEmpty().ifBlank { mac }) }
        }
    }

    private fun handleShare(intent: Intent?) {
        val uris = mutableListOf<Uri>()
        when (intent?.action) {
            Intent.ACTION_SEND -> shareUri(intent)?.let { uris += it }
            Intent.ACTION_SEND_MULTIPLE -> shareUris(intent)?.let { uris += it }
        }
        if (uris.isEmpty()) return
        scope.launch {
            uris.forEachIndexed { index, uri ->
                val name = shareDisplayName(uri)
                val size = shareSize(uri)
                val row = transferRegistry.beginLocalIngest(name, size, JobKind.Share)
                runCatching {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("unreadable")
                    transferRegistry.receiveFinished(row.id, bytes.size.toLong())
                    val photo = catalog.ingestBytes(
                        bytes,
                        if (name.contains('.')) name else "$name.jpg",
                        "share",
                        id = row.photoId,
                        onPhase = { phase ->
                            when (phase) {
                                "transcoding" -> transferRegistry.transcoding(row.id)
                                "ingesting" -> transferRegistry.ingesting(row.id)
                            }
                        },
                    )
                    transferRegistry.done(row.id)
                    runCatching { automation.onIngested(photo.id, "share") }
                        .onFailure { activityLog.record("automation", "onIngested failed", "error", it.message) }
                    if (index == uris.lastIndex) openedId.value = photo.id
                }.onFailure {
                    transferRegistry.failed(row.id, it.message ?: it.toString())
                    activityLog.record("share", "ingest failed", "error", it.message)
                }
            }
        }
    }

    private fun shareDisplayName(uri: Uri): String {
        val provided = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
        return provided?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "share.jpg"
    }

    private fun shareSize(uri: Uri): Long? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0).takeIf { it > 0L } else null }
    }.getOrNull()

    private fun shareUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun shareUris(intent: Intent): List<Uri>? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }
}
