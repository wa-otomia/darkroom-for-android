package io.github.wa_otomia.darkroom.core

import io.github.wa_otomia.darkroom.data.ftp.CameraFtpServer
import io.github.wa_otomia.darkroom.data.ftp.FtpHooks
import io.github.wa_otomia.darkroom.data.ftp.storFailureReason
import io.github.wa_otomia.darkroom.data.progress.JobKind
import io.github.wa_otomia.darkroom.data.progress.MemoryProgressStats
import io.github.wa_otomia.darkroom.data.transfer.TransferRegistry
import io.github.wa_otomia.darkroom.data.transfer.TransferState
import io.github.wa_otomia.darkroom.data.transfer.isInterruptedByStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TransferStopTest {
    @Test
    fun openFtpReceivingIsSelected() {
        assertTrue(
            isInterruptedByStop(
                TransferState.Receiving,
                JobKind.FtpTransfer,
                receiveClosed = false,
            ),
        )
    }

    @Test
    fun closedReceiveLaterPhasesDoneFailedAndShareAreNotSelected() {
        assertFalse(
            isInterruptedByStop(
                TransferState.Receiving,
                JobKind.FtpTransfer,
                receiveClosed = true,
            ),
        )
        assertFalse(isInterruptedByStop(TransferState.Transcoding, JobKind.FtpTransfer, receiveClosed = false))
        assertFalse(isInterruptedByStop(TransferState.Ingesting, JobKind.FtpTransfer, receiveClosed = false))
        assertFalse(isInterruptedByStop(TransferState.Done, JobKind.FtpTransfer, receiveClosed = false))
        assertFalse(isInterruptedByStop(TransferState.Failed, JobKind.FtpTransfer, receiveClosed = false))
        assertFalse(isInterruptedByStop(TransferState.Receiving, JobKind.Share, receiveClosed = false))
    }

    @Test
    fun storFailureReasonFollowsServerRunning() {
        assertEquals("文件不完整", storFailureReason(serverRunning = true))
        assertEquals("传输中断", storFailureReason(serverRunning = false))
    }

    @Test
    fun interruptReceivingFailsOnlyOpenFtpReceiveAndThenStaysTerminal() {
        withRegistry { registry ->
            val receiving = registry.transferStarted("session", "open.jpg", 100L)
            val closed = registry.transferStarted("session", "closed.jpg", 80L)
            registry.receiveFinished(closed.id, 80L)
            val transcoding = registry.transferStarted("session", "transcode.jpg", 70L)
            registry.transcoding(transcoding.id)
            val ingesting = registry.transferStarted("session", "ingest.jpg", 60L)
            registry.ingesting(ingesting.id)
            val done = registry.transferStarted("session", "done.jpg", 50L)
            registry.done(done.id)
            val failed = registry.transferStarted("session", "failed.jpg", 40L)
            registry.failed(failed.id, "文件不完整")
            val share = registry.beginLocalIngest("share.jpg", 30L, JobKind.Share)

            registry.interruptReceiving()

            assertTrue(registry.isInterrupted(receiving.id))
            assertEquals(TransferState.Failed, registry.rowState(receiving.id))
            assertEquals("传输中断", registry.rowError(receiving.id))
            assertFalse(registry.isInterrupted(closed.id))
            assertFalse(registry.isInterrupted(transcoding.id))
            assertFalse(registry.isInterrupted(ingesting.id))
            assertFalse(registry.isInterrupted(done.id))
            assertFalse(registry.isInterrupted(failed.id))
            assertFalse(registry.isInterrupted(share.id))
            assertEquals(TransferState.Transcoding, registry.rowState(transcoding.id))
            assertEquals(TransferState.Ingesting, registry.rowState(ingesting.id))
            assertEquals(TransferState.Done, registry.rowState(done.id))
            assertEquals(TransferState.Failed, registry.rowState(failed.id))
            assertEquals("文件不完整", registry.rowError(failed.id))
            assertEquals(TransferState.Receiving, registry.rowState(share.id))

            registry.receiveFinished(receiving.id, 100L)
            registry.transcoding(receiving.id)
            registry.ingesting(receiving.id)
            registry.done(receiving.id)
            registry.failed(receiving.id, "文件不完整")
            registry.interruptReceiving()
            assertEquals(TransferState.Failed, registry.rowState(receiving.id))
            assertEquals("传输中断", registry.rowError(receiving.id))
            assertTrue(registry.isInterrupted(receiving.id))

            registry.dismiss(receiving.id)
            assertFalse(registry.isInterrupted(receiving.id))
            assertTrue(registry.transfers.value.none { it.id == receiving.id })
        }
    }

    @Test
    fun closedReceiveStillAdvancesAfterInterrupt() {
        withRegistry { registry ->
            val closed = registry.transferStarted("session", "closed.jpg", 20L)
            registry.receiveFinished(closed.id, 20L)
            registry.interruptReceiving()
            assertFalse(registry.isInterrupted(closed.id))
            registry.transcoding(closed.id)
            assertEquals(TransferState.Transcoding, registry.rowState(closed.id))
            registry.ingesting(closed.id)
            assertEquals(TransferState.Ingesting, registry.rowState(closed.id))
            registry.done(closed.id)
            assertEquals(TransferState.Done, registry.rowState(closed.id))
        }
    }

    @Test(timeout = 20_000)
    fun aborThenPasvStorListAndRetrStillOpenDataConnection() {
        withFtpServer { _, ftp, inbox, _ ->
            ftp.command("ABOR")
            assertCode("226", ftp.readReply())

            val payload = byteArrayOf(1, 2, 3, 4, 5)
            assertCode("226", ftp.stor("photo.jpg", payload))
            assertArrayEquals(payload, File(inbox, "photo.jpg").readBytes())

            ftp.command("ABOR")
            assertCode("226", ftp.readReply())
            val listing = ftp.list()
            assertTrue(listing, listing.contains("photo.jpg"))
            assertArrayEquals(payload, ftp.retr("photo.jpg", payload.size))
        }
    }

    @Test(timeout = 20_000)
    fun incompleteStorThenListAndNextStorStillOpenDataConnection() {
        withFtpServer { server, ftp, inbox, hooks ->
            ftp.command("ALLO 64")
            assertCode("200", ftp.readReply())
            assertCode("426", ftp.stor("partial.jpg", ByteArray(8)))
            assertEquals(listOf("文件不完整"), hooks.failures)
            assertFalse(File(inbox, "partial.jpg").exists())
            assertTrue(server.status.listening)

            val listing = ftp.list()
            assertFalse(listing, listing.contains("partial.jpg"))

            val payload = byteArrayOf(9, 8, 7)
            assertCode("226", ftp.stor("next.jpg", payload, epsv = true))
            assertArrayEquals(payload, File(inbox, "next.jpg").readBytes())
            assertEquals(listOf("文件不完整"), hooks.failures)
        }
    }

    @Test(timeout = 20_000)
    fun aborDuringStorThenNextStorStillOpensDataConnection() {
        withFtpServer { _, ftp, inbox, hooks ->
            val data = ftp.openData()
            try {
                ftp.command("STOR hung.jpg")
                assertCode("150", ftp.readReply())
                data.getOutputStream().write(byteArrayOf(1, 2, 3))
                data.getOutputStream().flush()
                ftp.command("ABOR")
                assertCode("426", ftp.readReply())
            } finally {
                data.close()
            }
            assertEquals(listOf("文件不完整"), hooks.failures)
            assertFalse(File(inbox, "hung.jpg").exists())

            val payload = byteArrayOf(4, 5)
            assertCode("226", ftp.stor("after.jpg", payload))
            assertArrayEquals(payload, File(inbox, "after.jpg").readBytes())
        }
    }

    private fun withRegistry(block: (TransferRegistry) -> Unit) {
        val dispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "transfer-stop-test").apply { isDaemon = true }
        }.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            block(TransferRegistry(scope, MemoryProgressStats()))
        } finally {
            scope.cancel()
            dispatcher.close()
        }
    }

    private fun TransferRegistry.rowState(id: String): TransferState =
        transfers.value.single { it.id == id }.state

    private fun TransferRegistry.rowError(id: String): String? =
        transfers.value.single { it.id == id }.error

    private fun withFtpServer(
        block: (CameraFtpServer, LoopbackFtp, File, RecordingHooks) -> Unit,
    ) {
        val inbox = Files.createTempDirectory("transfer-stop-ftp").toFile()
        val hooks = RecordingHooks()
        val user = "cam"
        val password = UUID.randomUUID().toString()
        var server: CameraFtpServer? = null
        var control: Socket? = null
        try {
            val started = startLoopback(inbox, hooks, user, password)
            server = started.first
            control = Socket()
            control.tcpNoDelay = true
            control.soTimeout = 5_000
            control.connect(InetSocketAddress("127.0.0.1", started.second), 5_000)
            val ftp = LoopbackFtp(control)
            ftp.login(user, password)
            block(server, ftp, inbox, hooks)
        } finally {
            try {
                control?.close()
            } catch (_: Exception) {
            }
            try {
                server?.stop()
            } catch (_: Exception) {
            }
            inbox.deleteRecursively()
        }
    }

    private fun startLoopback(
        inbox: File,
        hooks: RecordingHooks,
        user: String,
        password: String,
    ): Pair<CameraFtpServer, Int> {
        var last: Exception? = null
        repeat(5) {
            val port = ServerSocket(0).use { it.localPort }
            val server = CameraFtpServer(
                bind = "127.0.0.1",
                port = port,
                user = user,
                password = password,
                pasvMin = 0,
                pasvMax = 0,
                inbox = inbox,
                onUploaded = { _, _ -> },
                hooks = hooks,
            )
            server.admitInterfaceForTest { true }
            try {
                server.start()
                return server to port
            } catch (e: Exception) {
                last = e
                try {
                    server.stop()
                } catch (_: Exception) {
                }
            }
        }
        throw last ?: IllegalStateException("ftp test bind failed")
    }

    private fun assertCode(code: String, reply: String) {
        assertTrue(reply, reply.startsWith(code))
    }

    private class RecordingHooks : FtpHooks by FtpHooks.NOOP {
        val failures = CopyOnWriteArrayList<String>()
        private val ids = AtomicInteger()

        override fun transferStarted(sessionId: String, filename: String, total: Long?): String =
            "t${ids.incrementAndGet()}"

        override fun transferFailed(transferId: String, error: String) {
            failures += error
        }
    }

    private inner class LoopbackFtp(socket: Socket) {
        private val input = BufferedInputStream(socket.getInputStream())
        private val output = BufferedOutputStream(socket.getOutputStream())

        fun login(user: String, password: String) {
            assertCode("220", readReply())
            command("USER $user")
            assertCode("331", readReply())
            command("PASS $password")
            assertCode("230", readReply())
        }

        fun command(line: String) {
            output.write("$line\r\n".toByteArray(StandardCharsets.US_ASCII))
            output.flush()
        }

        fun readReply(): String =
            readLine() ?: error("control connection closed")

        fun openData(epsv: Boolean = false): Socket {
            val port = if (epsv) {
                command("EPSV")
                val reply = readReply()
                assertCode("229", reply)
                epsvPort(reply)
            } else {
                command("PASV")
                val reply = readReply()
                assertCode("227", reply)
                pasvPort(reply)
            }
            return Socket().also { data ->
                data.tcpNoDelay = true
                data.soTimeout = 5_000
                // PASV may advertise a LAN host; the listener is on the control local address.
                data.connect(InetSocketAddress("127.0.0.1", port), 5_000)
            }
        }

        fun stor(name: String, bytes: ByteArray, epsv: Boolean = false): String {
            val data = openData(epsv)
            try {
                command("STOR $name")
                assertCode("150", readReply())
                data.getOutputStream().write(bytes)
                data.getOutputStream().flush()
            } finally {
                data.close()
            }
            return readReply()
        }

        fun list(): String {
            val data = openData()
            try {
                command("LIST")
                assertCode("150", readReply())
                val listing = data.getInputStream().readBytes().toString(StandardCharsets.UTF_8)
                assertCode("226", readReply())
                return listing
            } finally {
                data.close()
            }
        }

        fun retr(name: String, expected: Int): ByteArray {
            val data = openData()
            try {
                command("RETR $name")
                assertCode("150", readReply())
                val bytes = readFully(data.getInputStream(), expected)
                assertCode("226", readReply())
                return bytes
            } finally {
                data.close()
            }
        }

        private fun readLine(): String? {
            val buf = ByteArrayOutputStream()
            while (true) {
                val b = input.read()
                if (b < 0) return if (buf.size() == 0) null else buf.toString(StandardCharsets.UTF_8.name())
                if (b == '\n'.code) break
                if (b != '\r'.code) buf.write(b)
            }
            return buf.toString(StandardCharsets.UTF_8.name())
        }

        private fun pasvPort(reply: String): Int {
            val match = Regex("""\((\d+),(\d+),(\d+),(\d+),(\d+),(\d+)\)""").find(reply)
                ?: error("no pasv endpoint in $reply")
            return match.groupValues[5].toInt() * 256 + match.groupValues[6].toInt()
        }

        private fun epsvPort(reply: String): Int {
            val match = Regex("""\|(\d+)\|""").find(reply) ?: error("no epsv port in $reply")
            return match.groupValues[1].toInt()
        }
    }
}

private fun readFully(input: InputStream, expected: Int): ByteArray {
    val out = ByteArray(expected)
    var off = 0
    while (off < expected) {
        val n = input.read(out, off, expected - off)
        if (n < 0) error("data closed after $off of $expected bytes")
        off += n
    }
    return out
}
