package io.github.wa_otomia.darkroom.core

/**
 * Pure foreground-service time-limit pause policy. This file must not import
 * Android types so JVM unit tests can load it. [FGS_START_NOT_ALLOWED_SDK] is
 * the API level where the system starts rejecting background foreground starts
 * (same value as VERSION_CODES.S).
 */
const val FGS_START_NOT_ALLOWED_SDK = 31

data class FgsPause(
    val pausedAt: Long,
    val ftpWasListening: Boolean,
)

/**
 * Keeps the first pause time and ORs the listening flag, so a later keep-alive
 * or jobs write of false cannot clear an FTP pause or move its timestamp.
 */
fun mergeFgsPause(existing: FgsPause?, now: Long, ftpWasListening: Boolean): FgsPause {
    if (existing == null) return FgsPause(pausedAt = now, ftpWasListening = ftpWasListening)
    return existing.copy(ftpWasListening = existing.ftpWasListening || ftpWasListening)
}

enum class FgsService { Ftp, KeepAlive, Jobs }

enum class FgsStartOrigin {
    ProcessAutoStart,
    StickyRestart,
    JobsBusy,
    VisibleResume,
    UserAction,
}

/** Unpaused starts are always allowed. While paused, only a visible resume or a user action may start. */
fun fgsStartAllowed(origin: FgsStartOrigin, paused: Boolean): Boolean = when {
    !paused -> true
    origin == FgsStartOrigin.VisibleResume || origin == FgsStartOrigin.UserAction -> true
    else -> false
}

enum class FgsStopStep {
    StopListening,
    RemoveForeground,
    StopSelf,
    RecordPause,
    NotifyFtpPaused,
}

data class FgsStopPlan(
    val steps: List<FgsStopStep>,
    val ftpWasListening: Boolean,
)

/**
 * Stop steps come before the pause write. FTP appends the pause notice only
 * when it was listening. Keep-alive and jobs never change that flag.
 */
fun fgsTimeoutPlan(service: FgsService, ftpListening: Boolean = false): FgsStopPlan = when (service) {
    FgsService.Ftp -> {
        val stopped = listOf(
            FgsStopStep.StopListening,
            FgsStopStep.RemoveForeground,
            FgsStopStep.StopSelf,
            FgsStopStep.RecordPause,
        )
        FgsStopPlan(
            steps = if (ftpListening) stopped + FgsStopStep.NotifyFtpPaused else stopped,
            ftpWasListening = ftpListening,
        )
    }
    FgsService.KeepAlive, FgsService.Jobs -> FgsStopPlan(
        steps = listOf(
            FgsStopStep.RemoveForeground,
            FgsStopStep.StopSelf,
            FgsStopStep.RecordPause,
        ),
        ftpWasListening = false,
    )
}

/**
 * A rejected FTP start is recorded as listening: the user wanted it running.
 * [inService] appends [FgsStopStep.StopSelf] after the record (and FTP notice).
 */
fun fgsRejectedPlan(service: FgsService, inService: Boolean): FgsStopPlan {
    val ftp = service == FgsService.Ftp
    val recorded = listOf(FgsStopStep.RecordPause) +
        (if (ftp) listOf(FgsStopStep.NotifyFtpPaused) else emptyList())
    return FgsStopPlan(
        steps = if (inService) recorded + FgsStopStep.StopSelf else recorded,
        ftpWasListening = ftp,
    )
}

/**
 * Evaluates [matches] only at [FGS_START_NOT_ALLOWED_SDK] and above, so API 29/30
 * never touch the rejection exception type.
 */
fun isFgsStartNotAllowed(sdkInt: Int, matches: () -> Boolean): Boolean =
    sdkInt >= FGS_START_NOT_ALLOWED_SDK && matches()

enum class FtpAutoStartSkip {
    LocalNetworkPermission,
    NotificationPermission,
}

/** Local-network permission first, then notifications, matching the existing auto-start gate. */
fun ftpAutoStartSkip(localNetworkGranted: Boolean, notificationsGranted: Boolean): FtpAutoStartSkip? = when {
    !localNetworkGranted -> FtpAutoStartSkip.LocalNetworkPermission
    !notificationsGranted -> FtpAutoStartSkip.NotificationPermission
    else -> null
}

data class FgsResumePlan(
    val clearPause: Boolean,
    val startFtp: Boolean,
    val ftpSkip: FtpAutoStartSkip?,
    val cancelFtpNotice: Boolean,
    val startKeepAlive: Boolean,
    val reevaluateJobs: Boolean,
)

/**
 * No pause means no action. A pause is always cleared and jobs are reevaluated.
 * FTP restarts only if it was listening and the auto-start gate has no skip;
 * otherwise the pause notice is cancelled. [ftpSkip] is kept only to log a
 * skip of an FTP server that was listening.
 */
fun fgsResumePlan(
    pause: FgsPause?,
    keepAliveEnabled: Boolean,
    ftpSkip: FtpAutoStartSkip?,
): FgsResumePlan {
    if (pause == null) {
        return FgsResumePlan(
            clearPause = false,
            startFtp = false,
            ftpSkip = null,
            cancelFtpNotice = false,
            startKeepAlive = false,
            reevaluateJobs = false,
        )
    }
    val startFtp = pause.ftpWasListening && ftpSkip == null
    return FgsResumePlan(
        clearPause = true,
        startFtp = startFtp,
        ftpSkip = if (pause.ftpWasListening) ftpSkip else null,
        cancelFtpNotice = !startFtp,
        startKeepAlive = keepAliveEnabled,
        reevaluateJobs = true,
    )
}
