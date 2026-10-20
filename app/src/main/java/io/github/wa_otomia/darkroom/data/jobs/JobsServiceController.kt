package io.github.wa_otomia.darkroom.data.jobs

import android.content.Context
import io.github.wa_otomia.darkroom.core.FgsService
import io.github.wa_otomia.darkroom.core.FgsStartOrigin
import io.github.wa_otomia.darkroom.core.fgsRejectedPlan
import io.github.wa_otomia.darkroom.core.fgsStartAllowed
import io.github.wa_otomia.darkroom.data.printer.PrintQueue
import io.github.wa_otomia.darkroom.data.settings.ActivityLog
import io.github.wa_otomia.darkroom.data.settings.FgsPauseCause
import io.github.wa_otomia.darkroom.data.settings.ForegroundPause
import io.github.wa_otomia.darkroom.data.settings.isForegroundStartNotAllowed
import io.github.wa_otomia.darkroom.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobsServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printQueue: PrintQueue,
    private val aiJobs: AiJobs,
    private val foregroundPause: ForegroundPause,
    private val activityLog: ActivityLog,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private var stopJob: Job? = null
    @Volatile private var serviceRunning = false
    private val reevaluateTick = MutableStateFlow(0)
    private var pauseSkipLogged = false

    /**
     * [onServiceStopped] runs on the service thread. [onCounts] stays on the collector.
     * Both only mutate [stopJob] and [serviceRunning] under this lock, and never call each other.
     */
    private val runningGate = Any()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            combine(printQueue.printJobs, aiJobs.jobs, reevaluateTick) { prints, ais, _ ->
                val printBusy = prints.count {
                    it.state == PrintQueue.STATE_QUEUED || it.state == PrintQueue.STATE_RUNNING
                }
                printBusy to ais.size
            }.collect { (printBusy, aiBusy) ->
                onCounts(printBusy, aiBusy)
            }
        }
    }

    /** Bumps the collector tick so [onCounts] runs again on that coroutine, not on the caller. */
    fun reevaluate() {
        reevaluateTick.update { it + 1 }
    }

    /** Foreground start was rejected, or the service stopped itself. Does not cancel print or AI jobs. */
    fun onServiceStopped() {
        synchronized(runningGate) {
            stopJob?.cancel()
            stopJob = null
            serviceRunning = false
        }
    }

    private fun onCounts(printBusy: Int, aiBusy: Int) {
        val busy = printBusy > 0 || aiBusy > 0
        if (busy) {
            if (!fgsStartAllowed(FgsStartOrigin.JobsBusy, foregroundPause.current() != null)) {
                synchronized(runningGate) {
                    stopJob?.cancel()
                    stopJob = null
                    serviceRunning = false
                }
                logPauseSkipOnce()
                return
            }
            pauseSkipLogged = false
            // Set running before start() so an in-service rejection can clear it afterwards.
            synchronized(runningGate) {
                stopJob?.cancel()
                stopJob = null
                serviceRunning = true
            }
            try {
                JobsForegroundService.start(context)
            } catch (e: Exception) {
                synchronized(runningGate) {
                    serviceRunning = false
                }
                if (!e.isForegroundStartNotAllowed()) throw e
                val plan = fgsRejectedPlan(FgsService.Jobs, inService = false)
                foregroundPause.record(FgsService.Jobs, FgsPauseCause.StartRejected, plan.ftpWasListening)
            }
        } else if (isServiceRunning()) {
            val idleStop = appScope.launch {
                delay(IDLE_STOP_MS)
                val failure = runCatching { JobsForegroundService.stop(context) }.exceptionOrNull()
                if (failure is CancellationException) throw failure
                synchronized(runningGate) {
                    serviceRunning = false
                }
            }
            synchronized(runningGate) {
                if (!serviceRunning) {
                    idleStop.cancel()
                } else {
                    stopJob?.cancel()
                    stopJob = idleStop
                }
            }
        }
    }

    private fun isServiceRunning(): Boolean = synchronized(runningGate) { serviceRunning }

    private fun logPauseSkipOnce() {
        if (pauseSkipLogged) return
        pauseSkipLogged = true
        try {
            activityLog.record("jobs", "start skipped: paused by system time limit")
        } catch (_: Exception) {
            pauseSkipLogged = false
        }
    }

    companion object {
        private const val IDLE_STOP_MS = 3_000L
    }
}
