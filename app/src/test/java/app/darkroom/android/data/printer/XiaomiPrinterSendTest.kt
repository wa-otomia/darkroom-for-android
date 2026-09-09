package app.darkroom.android.data.printer

import app.darkroom.android.core.CHANNEL_DATA_ENC
import app.darkroom.android.core.CHANNEL_FILE_ENC
import app.darkroom.android.core.CHUNK_BYTES
import app.darkroom.android.core.ENCODING_JSON
import app.darkroom.android.core.ENCRYPT_ECB_OFFSET
import app.darkroom.android.core.INTERACTIVE_RESPONSE
import app.darkroom.android.core.buildFrame
import app.darkroom.android.core.decryptEcb
import app.darkroom.android.core.encryptEcb
import app.darkroom.android.core.parseFrame
import app.darkroom.android.core.readU16LE
import app.darkroom.android.core.readU32LE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.ArrayDeque

/**
 * Drives [XiaomiPrinter.printJpeg] against an in-memory pipe: the fake answers `print_job`
 * with a job id, then records every socket write of the channel-4 upload.
 */
class XiaomiPrinterSendTest {
    private val key = ByteArray(16) { (it + 1).toByte() }

    private class FakePipe(private val key: ByteArray, private val failWritesAt: Set<Int> = emptySet()) : BytePipe {
        val writes = ArrayList<ByteArray>()
        val writeTimes = ArrayList<Long>()
        private val inbox = ArrayDeque<Byte>()
        var attempts = 0

        override fun write(data: ByteArray) {
            attempts += 1
            if (attempts in failWritesAt) throw IOException("EIO")
            writes += data
            writeTimes += System.nanoTime()
            // First write is the encrypted print_job RPC; answer with a job id.
            if (writes.size == 1) {
                val frame = parseFrame(data)
                val req = String(decryptEcb(key, frame.body, stripZeros = true), Charsets.UTF_8)
                val id = Regex("\"id\":(\\d+)").find(req)!!.groupValues[1].toInt()
                val body = encryptEcb(key, """{"result":{"job_id":42},"id":$id}""".toByteArray())
                val reply = buildFrame(CHANNEL_DATA_ENC, INTERACTIVE_RESPONSE, ENCODING_JSON, id, id, ENCRYPT_ECB_OFFSET, body)
                reply.forEach { inbox.addLast(it) }
            }
        }

        override fun readExact(n: Int, timeoutMs: Int): ByteArray {
            if (inbox.size < n) error("read timeout")
            return ByteArray(n) { inbox.removeFirst() }
        }

        override fun close() = Unit
    }

    private fun printer(pipe: BytePipe, pace: Long = 0L, perWrite: Int = XiaomiPrinter.MAX_FRAMES_PER_WRITE) =
        XiaomiPrinter(pipe, readTimeout = 1_000, interWritePaceMs = pace, framesPerWrite = perWrite, log = {}).also {
            it.useSessionKey(key)
        }

    /** Splits a coalesced write back into frames using the msgAttr body length. */
    private fun splitFrames(blob: ByteArray): List<ByteArray> {
        val out = ArrayList<ByteArray>()
        var off = 0
        while (off < blob.size) {
            val attr = readU16LE(blob, off + 18)
            val bodyLen = app.darkroom.android.core.bodyLengthFromAttr(attr)
            val len = 22 + bodyLen
            out += blob.copyOfRange(off, off + len)
            off += len
        }
        return out
    }

    @Test
    fun coalescesThreeFramesPerWriteAndCoversWholeFile() {
        val jpeg = ByteArray(CHUNK_BYTES * 7 + 100) { (it * 31).toByte() }
        val pipe = FakePipe(key)
        val progress = ArrayList<Pair<Int, Int>>()
        val jobId = printer(pipe).printJpeg(jpeg) { c, t -> progress += c to t }

        assertEquals(42, jobId)
        val totalChunks = 8
        val uploads = pipe.writes.drop(1)
        // 8 frames → writes of 3, 3, 2.
        assertEquals(listOf(3, 3, 2), uploads.map { splitFrames(it).size })
        val frames = uploads.flatMap { splitFrames(it) }.map { parseFrame(it) }
        assertEquals(totalChunks, frames.size)
        frames.forEachIndexed { i, f ->
            assertEquals(CHANNEL_FILE_ENC, f.channelId)
            assertEquals(totalChunks, f.pkgTotal)
            assertEquals(i + 1, f.pkgNum)
            assertTrue("multi-packet flag", f.msgAttr >= 8192)
        }
        // Decrypt and reassemble: job id prefix then the original bytes.
        val rebuilt = frames.flatMap { f ->
            val plain = decryptEcb(key, f.body)
            assertEquals(42, readU32LE(plain, 0))
            val len = if (f.pkgNum == totalChunks) jpeg.size - CHUNK_BYTES * (totalChunks - 1) else CHUNK_BYTES
            plain.copyOfRange(4, 4 + len).toList()
        }
        assertEquals(jpeg.toList(), rebuilt)
        assertEquals(totalChunks to totalChunks, progress.last())
    }

    @Test
    fun noPacingSleepBetweenWritesByDefault() {
        val jpeg = ByteArray(CHUNK_BYTES * 30)
        val pipe = FakePipe(key)
        val p = printer(pipe)
        p.printJpeg(jpeg)
        val stats = p.lastSendStats!!
        assertEquals(jpeg.size, stats.bytes)
        assertEquals(30, stats.frames)
        assertEquals(10, stats.writes)
        // 10 writes with the old 65 ms pacing would take ≥ 585 ms; unpaced it is a few ms.
        assertTrue("elapsed ${stats.elapsedMs} ms", stats.elapsedMs < 300)
        val gaps = pipe.writeTimes.zipWithNext { a, b -> (b - a) / 1_000_000 }
        assertTrue("max gap ${gaps.maxOrNull()} ms", (gaps.maxOrNull() ?: 0L) < 50L)
    }

    @Test
    fun optionalPacingIsHonoured() {
        val jpeg = ByteArray(CHUNK_BYTES * 6)
        val pipe = FakePipe(key)
        val p = printer(pipe, pace = 40L)
        p.printJpeg(jpeg)
        // 2 writes → one 40 ms pause between them, none after the last.
        val gaps = pipe.writeTimes.drop(1).zipWithNext { a, b -> (b - a) / 1_000_000 }
        assertEquals(1, gaps.size)
        assertTrue("gap ${gaps[0]} ms", gaps[0] >= 35L)
    }

    @Test
    fun transientWriteFailureIsRetried() {
        val jpeg = ByteArray(CHUNK_BYTES * 3)
        // Attempt 1 = print_job RPC, attempt 2 = first upload write → fail once, then succeed.
        val pipe = FakePipe(key, failWritesAt = setOf(2))
        printer(pipe).printJpeg(jpeg)
        assertEquals(2, pipe.writes.size)
        assertEquals(3, splitFrames(pipe.writes[1]).size)
    }

    @Test(expected = IOException::class)
    fun persistentWriteFailureGivesUpAfterRetries() {
        val jpeg = ByteArray(CHUNK_BYTES)
        val pipe = FakePipe(key, failWritesAt = setOf(2, 3, 4))
        printer(pipe).printJpeg(jpeg)
    }
}
