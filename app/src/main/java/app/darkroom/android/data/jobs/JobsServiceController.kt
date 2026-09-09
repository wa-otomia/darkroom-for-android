package app.darkroom.android.data.jobs

import android.content.Context
import app.darkroom.android.data.printer.PrintQueue
import app.darkroom.android.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobsServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val printQueue: PrintQueue,
    private val aiJobs: AiJobs,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private var stopJob: Job? = null
    @Volatile private var serviceRunning = false

    fun start() {
        if (!started.compareAndSet(false, true)) return
        appScope.launch {
            combine(printQueue.printJobs, aiJobs.jobs) { prints, ais ->
                val printBusy = prints.count {
                    it.state == PrintQueue.STATE_QUEUED || it.state == PrintQueue.STATE_RUNNING
                }
                printBusy to ais.size
            }.collect { (printBusy, aiBusy) ->
                onCounts(printBusy, aiBusy)
            }
        }
    }

    private fun onCounts(printBusy: Int, aiBusy: Int) {
        val busy = printBusy > 0 || aiBusy > 0
        if (busy) {
            stopJob?.cancel()
            stopJob = null
            JobsForegroundService.start(context)
            serviceRunning = true
        } else if (serviceRunning) {
            stopJob?.cancel()
            stopJob = appScope.launch {
                delay(IDLE_STOP_MS)
                JobsForegroundService.stop(context)
                serviceRunning = false
            }
        }
    }

    companion object {
        private const val IDLE_STOP_MS = 3_000L
    }
}
