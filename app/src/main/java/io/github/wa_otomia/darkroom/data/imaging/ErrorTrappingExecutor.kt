package io.github.wa_otomia.darkroom.data.imaging

import java.util.concurrent.Executor

/**
 * Runs each command on [delegate], handing anything it throws (an [Error] included) to [onError]
 * instead of letting it reach the worker thread's uncaught-exception handler. Used as the ML Kit
 * segmenter's executor so a native-library or out-of-memory error during model load or inference
 * ends the local job as a failure rather than killing the process (EDID040-R11, design D4).
 */
internal class ErrorTrappingExecutor(
    private val delegate: Executor,
    private val onError: (Throwable) -> Unit,
) : Executor {
    override fun execute(command: Runnable) {
        delegate.execute {
            try {
                command.run()
            } catch (t: Throwable) {
                runCatching { onError(t) }
            }
        }
    }
}
