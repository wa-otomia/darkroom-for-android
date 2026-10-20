package io.github.wa_otomia.darkroom.data.settings

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.wa_otomia.darkroom.core.FgsPause
import io.github.wa_otomia.darkroom.core.FgsService
import io.github.wa_otomia.darkroom.core.isFgsStartNotAllowed
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Why a foreground-service time-limit pause was recorded. */
enum class FgsPauseCause {
    Timeout,
    StartRejected,
}

/**
 * Shared pause record for companions and services that are not Hilt entry points.
 * [current], [record] and [clear] never throw. Logs name the service and cause
 * or the exception message, and never include a host, account or password.
 */
@Singleton
class ForegroundPause @Inject constructor(
    private val settings: SettingsRepository,
    private val activityLog: ActivityLog,
) {
    /** A read failure is treated as not paused; a later rejection still degrades. */
    fun current(): FgsPause? = try {
        settings.readForegroundPause()
    } catch (e: Exception) {
        logFailure("settings", "read foreground pause failed", e)
        null
    }

    /**
     * [ftpWasListening] comes from the caller's stop plan. A write failure is
     * logged and not rethrown.
     */
    fun record(service: FgsService, cause: FgsPauseCause, ftpWasListening: Boolean) {
        val kind = service.logKind()
        try {
            val wrote = settings.recordForegroundPause(System.currentTimeMillis(), ftpWasListening)
            if (!wrote) {
                logFailure(kind, "pause record write failed", null)
                return
            }
            activityLog.record(kind, cause.logMessage())
        } catch (e: Exception) {
            logFailure(kind, "pause record write failed", e)
        }
    }

    fun clear() {
        try {
            val cleared = settings.clearForegroundPause()
            if (!cleared) logFailure("settings", "clear foreground pause failed", null)
        } catch (e: Exception) {
            logFailure("settings", "clear foreground pause failed", e)
        }
    }

    private fun logFailure(kind: String, message: String, error: Exception?) {
        try {
            activityLog.record(kind, message, status = "error", error = error?.message)
        } catch (_: Exception) {
            // The activity log is best-effort. Callers must not observe this failure.
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ForegroundPauseEntryPoint {
    fun foregroundPause(): ForegroundPause
}

fun foregroundPause(context: Context): ForegroundPause =
    EntryPointAccessors.fromApplication(context, ForegroundPauseEntryPoint::class.java)
        .foregroundPause()

/**
 * True when this throwable is a foreground-start rejection. The predicate runs
 * only on API 31+, so older devices never load that exception type.
 */
fun Throwable.isForegroundStartNotAllowed(): Boolean =
    isFgsStartNotAllowed(Build.VERSION.SDK_INT) {
        Api31Impl.isStartNotAllowed(this@isForegroundStartNotAllowed)
    }

@RequiresApi(31)
private object Api31Impl {
    fun isStartNotAllowed(error: Throwable): Boolean =
        error is android.app.ForegroundServiceStartNotAllowedException
}

private fun FgsService.logKind(): String = when (this) {
    FgsService.Ftp -> "ftp"
    FgsService.KeepAlive -> "keepalive"
    FgsService.Jobs -> "jobs"
}

private fun FgsPauseCause.logMessage(): String = when (this) {
    FgsPauseCause.Timeout -> "paused by system time limit"
    FgsPauseCause.StartRejected -> "foreground start rejected by system"
}
