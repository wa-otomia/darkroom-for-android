package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.PRINTER_NO_PROGRESS_MS
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class StreamBytePipeTest {
    @Test fun timeoutDoesNotDiscardPartialBytes() {
        val input = PipedInputStream(); val sender = PipedOutputStream(input)
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close(); sender.close() }
        try {
            sender.write(0x7e); sender.flush()
            assertThrows(SocketTimeoutException::class.java) { pipe.readExact(2, 30) }
            sender.write(0x64); sender.flush()
            assertArrayEquals(byteArrayOf(0x7e, 0x64), pipe.readExact(2, 1_000))
        } finally { pipe.close() }
    }
    @Test fun negativeReadTimesOutWithoutDisconnect() {
        val closes = AtomicInteger()
        val input = object : InputStream() {
            override fun read(): Int = -1
            override fun read(b: ByteArray, off: Int, len: Int): Int = -1
        }
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { closes.incrementAndGet() }
        try {
            assertThrows(SocketTimeoutException::class.java) { pipe.readExact(1, 80) }
            assertEquals(0, closes.get())
        } finally { pipe.close() }
    }
    @Test fun negativeOrZeroReadRetriesUntilData() {
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val input = object : InputStream() {
            override fun read(): Int = error("unused")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                when (calls.getAndIncrement()) {
                    0 -> return -1
                    1 -> return 0
                    else -> {
                        if (!release.await(2, TimeUnit.SECONDS)) return -1
                        b[off] = 0x2a
                        return 1
                    }
                }
            }
        }
        val closes = AtomicInteger()
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { closes.incrementAndGet(); release.countDown() }
        try {
            release.countDown()
            assertArrayEquals(byteArrayOf(0x2a), pipe.readExact(1, 1_000))
            assertEquals(0, closes.get())
            assertTrue(calls.get() >= 3)
        } finally { pipe.close() }
    }
    @Test fun readFailureDoesNotFailWriteOrClose() {
        val input = object : InputStream() {
            private var delivered = false
            override fun read(): Int = throw IOException("read failed")
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (!delivered) {
                    delivered = true
                    b[off] = 0x11
                    return 1
                }
                throw IOException("read failed")
            }
        }
        val closes = AtomicInteger()
        var writes = 0
        val output = object : OutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) { writes++ }
            override fun write(b: Int) = error("unused")
        }
        val pipe = StreamBytePipe(input, output) { closes.incrementAndGet() }
        try {
            assertArrayEquals(byteArrayOf(0x11), pipe.readExact(1, 1_000))
            val error = assertThrows(IOException::class.java) { pipe.readExact(1, 1_000) }
            assertTrue(error.message?.contains("read failed") == true)
            assertFalse(error is EOFException)
            pipe.write(byteArrayOf(9))
            assertEquals(1, writes)
            assertEquals(0, closes.get())
        } finally { pipe.close() }
    }
    @Test fun closingUnblocksAWaitingReader() {
        val input = PipedInputStream(); val sender = PipedOutputStream(input)
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close(); sender.close() }
        pipe.close()
        assertThrows(IOException::class.java) { pipe.readExact(1, 100) }
    }
    @Test fun closeWakesWaitingReaderAndStopsBackoff() {
        val readsAfterClose = AtomicInteger()
        val closedFlag = java.util.concurrent.atomic.AtomicBoolean(false)
        val entered = CountDownLatch(1)
        val input = object : InputStream() {
            override fun read(): Int = -1
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                entered.countDown()
                if (closedFlag.get()) readsAfterClose.incrementAndGet()
                return -1
            }
        }
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { closedFlag.set(true) }
        val error = AtomicReference<Throwable>()
        val waiting = thread {
            try {
                pipe.readExact(1, 5_000)
            } catch (t: Throwable) {
                error.set(t)
            }
        }
        try {
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            Thread.sleep(30)
            val started = System.nanoTime()
            pipe.close()
            waiting.join(1_000)
            val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            assertFalse(waiting.isAlive)
            assertTrue("close did not wake the reader in ${elapsedMs}ms", elapsedMs < 500)
            val thrown = error.get()
            assertTrue(thrown is EOFException)
            assertTrue(thrown?.message?.contains("closed") == true)
            Thread.sleep(40)
            val after = readsAfterClose.get()
            Thread.sleep(80)
            assertEquals(after, readsAfterClose.get())
            assertTrue(after <= 1)
        } finally { pipe.close() }
    }
    @Test fun writeWatchdogClosesBlockedTransportWithoutRetry() {
        val closed = java.util.concurrent.CountDownLatch(1)
        val input = object : InputStream() {
            override fun read(): Int { closed.await(2, java.util.concurrent.TimeUnit.SECONDS); return -1 }
        }
        var writes = 0
        val output = object : OutputStream() {
            override fun write(b: Int) {
                writes++
                check(closed.await(2, java.util.concurrent.TimeUnit.SECONDS)) { "Watchdog did not run" }
                throw IOException("Closed by watchdog")
            }
        }
        val pipe = StreamBytePipe(input, output, 30) { closed.countDown() }
        try {
            assertThrows(IOException::class.java) { pipe.write(byteArrayOf(1)) }
            assertEquals(1, writes)
        } finally { pipe.close() }
    }
    @Test fun defaultWriteDeadlineIsSixtySeconds() {
        val input = ByteArrayInputStream(ByteArray(0))
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) {}
        try {
            assertEquals(60_000L, pipe.writeTimeoutMs)
            assertEquals(PRINTER_NO_PROGRESS_MS, pipe.writeTimeoutMs)
        } finally { pipe.close() }
    }
    @Test(timeout = 35_000)
    fun twentySecondWriteWithinDefaultDeadlineDoesNotClose() {
        val closes = AtomicInteger()
        val release = CountDownLatch(1)
        val started = CountDownLatch(1)
        var writes = 0
        val input = object : InputStream() {
            override fun read(): Int {
                release.await(30, TimeUnit.SECONDS)
                return -1
            }
        }
        val output = object : OutputStream() {
            override fun write(b: ByteArray, off: Int, len: Int) {
                writes++
                started.countDown()
                check(release.await(30, TimeUnit.SECONDS)) { "slow write was not released" }
            }
            override fun write(b: Int) = error("unused")
        }
        val pipe = StreamBytePipe(input, output) { closes.incrementAndGet(); release.countDown() }
        val failure = AtomicReference<Throwable>()
        val worker = thread {
            try {
                pipe.write(byteArrayOf(1, 2, 3))
            } catch (t: Throwable) {
                failure.set(t)
            }
        }
        try {
            assertEquals(60_000L, pipe.writeTimeoutMs)
            assertTrue(started.await(2, TimeUnit.SECONDS))
            Thread.sleep(20_000)
            assertEquals(0, closes.get())
            release.countDown()
            worker.join(2_000)
            assertFalse(worker.isAlive)
            assertNull(failure.get())
            assertEquals(1, writes)
            pipe.write(byteArrayOf(4))
            assertEquals(2, writes)
            assertEquals(0, closes.get())
        } finally {
            release.countDown()
            pipe.close()
        }
    }
}
