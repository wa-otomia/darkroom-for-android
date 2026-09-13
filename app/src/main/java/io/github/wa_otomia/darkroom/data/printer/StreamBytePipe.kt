package io.github.wa_otomia.darkroom.data.printer

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.concurrent.thread

/** A single bounded reader owns InputStream. Timeouts never discard partial bytes.
 * Bluetooth InputStream.read has no timeout; checking a clock before it is not enough. */
internal class StreamBytePipe(
    private val input: InputStream,
    private val output: OutputStream,
    private val writeTimeoutMs: Long = 15_000,
    private val closeTransport: () -> Unit,
) : BytePipe {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val bytes = ArrayDeque<Byte>()
    private var failure: IOException? = null
    private var closed = false
    init { require(writeTimeoutMs > 0) }
    private val reader = thread(start = true, isDaemon = true, name = "Darkroom-SPP-reader") {
        try {
            val buf = ByteArray(4096)
            while (true) {
                val count = input.read(buf)
                if (count < 0) throw EOFException("Printer disconnected")
                if (count == 0) continue
                lock.withLock {
                    if (closed) return@thread
                    if (bytes.size + count > 1024 * 1024) throw IOException("Printer receive buffer overflow")
                    repeat(count) { bytes.add(buf[it]) }
                    changed.signalAll()
                }
            }
        } catch (e: Exception) {
            lock.withLock {
                failure = if (e is IOException) e else IOException("Printer reader failed", e)
                changed.signalAll()
            }
        }
    }

    @Synchronized
    override fun write(data: ByteArray) {
        lock.withLock {
            if (closed) throw IOException("Printer connection closed")
            failure?.let { throw it }
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
                failure?.let { throw it }
                if (closed) throw EOFException("Printer connection closed")
                if (remaining <= 0) throw SocketTimeoutException("Printer read timeout")
                remaining = changed.awaitNanos(remaining)
            }
            ByteArray(n) { bytes.removeFirst() }
        }
    }

    companion object {
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
