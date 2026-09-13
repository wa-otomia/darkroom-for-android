package io.github.wa_otomia.darkroom.data.printer

import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.SocketTimeoutException

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
    @Test fun eofIsNotAnInfiniteWait() {
        val input = ByteArrayInputStream(byteArrayOf(1))
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close() }
        try {
            assertArrayEquals(byteArrayOf(1), pipe.readExact(1, 1_000))
            assertThrows(EOFException::class.java) { pipe.readExact(1, 1_000) }
        } finally { pipe.close() }
    }
    @Test fun closingUnblocksAWaitingReader() {
        val input = PipedInputStream(); val sender = PipedOutputStream(input)
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close(); sender.close() }
        pipe.close()
        assertThrows(IOException::class.java) { pipe.readExact(1, 100) }
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

}
