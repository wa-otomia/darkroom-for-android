package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.PRINTER_NO_PROGRESS_MS
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/** A single bounded reader owns InputStream. Timeouts never discard partial bytes.
 * A read of -1 or 0 is idle, not disconnect. Read failures are visible only to readers.
 * Bluetooth InputStream.read has no timeout; a watchdog closes a write that exceeds [writeTimeoutMs]. */
internal class StreamBytePipe(
    private val input: InputStream,
    private val output: OutputStream,
    internal val writeTimeoutMs: Long = PRINTER_NO_PROGRESS_MS,
    private val closeTransport: () -> Unit,
) : BytePipe {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val bytes = ArrayDeque<Byte>()
    private var readFailure: IOException? = null
    private var closed = false
    init { require(writeTimeoutMs > 0) }
    private val reader = thread(start = true, isDaemon = true, name = "Darkroom-SPP-reader") {
        val buf = ByteArray(4096)
        try {
            while (true) {
                if (lock.withLock { closed }) return@thread
                val count = input.read(buf)
                if (count <= 0) {
                    if (lock.withLock { closed }) return@thread
                    if (!idleBackoff()) return@thread
                    continue
                }
                lock.withLock {
                    if (closed) return@thread
                    if (bytes.size + count > 1024 * 1024) throw IOException("Printer receive buffer overflow")
                    repeat(count) { bytes.add(buf[it]) }
                    changed.signalAll()
                }
            }
        } catch (e: Exception) {
            latchReadFailure(if (e is IOException) e else IOException("Printer reader failed", e))
        }
    }

    /** About 15 ms, matching the pre-regression idle retry. Close signals and interrupts this wait. */
    private fun idleBackoff(): Boolean = lock.withLock {
        if (closed) return@withLock false
        try {
            changed.await(IDLE_BACKOFF_MS, TimeUnit.MILLISECONDS)
        } catch (_: InterruptedException) {
        }
        !closed
    }

    private fun latchReadFailure(error: IOException) {
        lock.withLock {
            if (!closed && readFailure == null) readFailure = error
            changed.signalAll()
        }
    }

    @Synchronized
    override fun write(data: ByteArray) {
        lock.withLock {
            if (closed) throw IOException("Printer connection closed")
        }
        // Closing BluetoothSocket interrupts blocked IO. No ambiguous write is replayed.
        val timedOut = AtomicBoolean(false)
        val watchdog = timeouts.schedule({
            timedOut.set(true)
            runCatching { close() }
        }, writeTimeoutMs, TimeUnit.MILLISECONDS)
        try {
            output.write(data)
            output.flush()
            if (timedOut.get()) throw SocketTimeoutException("Printer write timeout; delivery is unknown")
        } finally {
            watchdog.cancel(false)
        }
    }

    override fun readExact(n: Int, timeoutMs: Int): ByteArray {
        require(n in 0..1024 * 1024 && timeoutMs > 0)
        return lock.withLock {
            var remaining = timeoutMs.toLong() * 1_000_000
            while (bytes.size < n) {
                // Already-buffered bytes are returned above. A read-side failure does not discard them.
                readFailure?.let { throw it }
                if (closed) throw EOFException("Printer connection closed")
                if (remaining <= 0) throw SocketTimeoutException("Printer read timeout")
                remaining = changed.awaitNanos(remaining)
            }
            ByteArray(n) { bytes.removeFirst() }
        }
    }

    companion object {
        private const val IDLE_BACKOFF_MS = 15L
        private val timeouts = Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "Darkroom-SPP-timeouts").apply { isDaemon = true }
        }
    }

    override fun close() {
        lock.withLock {
            closed = true
            changed.signalAll()
        }
        try { closeTransport() } finally { reader.interrupt() }
    }
}
