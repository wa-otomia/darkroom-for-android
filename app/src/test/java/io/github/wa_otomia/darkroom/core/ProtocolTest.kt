package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.SecureRandom

class ProtocolTest {
    @Test
    fun helloFrameMatchesCapture() {
        val built = buildHello(1)
        assertEquals(CAPTURED_HELLO_HEX, toHex(built))
        assertTrue(verifyChecksum(built))
        assertEquals(CHANNEL_AUTH, built[3].toInt() and 0xFF)
        assertEquals(INTERACTIVE_CLIENT_HELLO, built[4].toInt() and 0xFF)
        val parsed = parseFrame(built)
        assertEquals("hello", String(parsed.body, Charsets.UTF_8))
        assertEquals(parsed.body.size, bodyLengthFromAttr(parsed.msgAttr))
    }

    @Test
    fun parseFrameUsesPocLength() {
        assertEquals(5, bodyLengthFromAttr(5 + ENCRYPT_ECB_OFFSET))
        assertEquals(32, bodyLengthFromAttr(32 + ENCRYPT_ECB_OFFSET))
        assertEquals(988, bodyLengthFromAttr(988 + ENCRYPT_ECB_OFFSET + MULTI_PACKET))
        val hello = buildHello(1)
        val parsed = parseFrame(hello)
        assertEquals(CHANNEL_AUTH, parsed.channelId)
        assertEquals(INTERACTIVE_CLIENT_HELLO, parsed.interactive)
        assertEquals(5, parsed.body.size)
    }

    @Test
    fun dhMutualKeyAndConfirm() {
        val p = BigInteger("FFFFFFFFFFFFFFC5", 16)
        val g = BigInteger.TWO
        val a = BigInteger.valueOf(123456789)
        val ra = modPow(g, a, p)
        val gField = byteArrayOf(0x32, 0, 0, 0)
        val pAscii = intToBytes(p).toString(Charsets.US_ASCII).padStart(16, '0').take(16)
        val raAscii = intToBytes(ra).toString(Charsets.US_ASCII).padStart(16, '0').take(16)
        val pField = pAscii.toByteArray(Charsets.US_ASCII)
        val raField = raAscii.toByteArray(Charsets.US_ASCII)
        val body = gField + pField + raField
        val info = deriveSession(body, BigInteger.valueOf(987654321))
        val rb = bytesToInt(info.rbField)
        val k = modPow(rb, a, p)
        var kb = intToBytes(k)
        val keyServer = if (kb.size < 16) pack16End(kb) else kb.copyOf(16)
        assertArrayEquals(keyServer, info.key)
        assertArrayEquals(pField, decryptEcb(keyServer, info.encP).copyOf(16))
    }

    @Test
    fun commandRoundtrip() {
        val key = ByteArray(16) { it.toByte() }
        val params = printJobParams(9, 1)
        val raw = encodeRpc("print_job", 5, params)
        val enc = encryptEcb(key, raw)
        val fr = buildFrame(
            channelId = CHANNEL_DATA_ENC,
            interactive = INTERACTIVE_REQUEST,
            encoding = ENCODING_JSON,
            arcMsgSn = 5,
            msgSn = 5,
            encryptOffset = ENCRYPT_ECB_OFFSET,
            body = enc,
        )
        val dec = decryptEcb(key, parseFrame(fr).body, stripZeros = true)
        val text = String(dec, Charsets.UTF_8)
        assertTrue(text.contains("\"method\":\"print_job\""))
        assertTrue(text.contains("\"channel\":64"))
    }

    @Test
    fun fileSplitRoundtrip() {
        val key = ByteArray(16) { it.toByte() }
        val original = ByteArray(7000).also { SecureRandom().nextBytes(it) }
        val jobId = 7
        val chunk = CHUNK_BYTES
        val total = (original.size + chunk - 1) / chunk
        val parts = mutableListOf<ByteArray>()
        for (j in 0 until total) {
            val from = j * chunk
            val to = minOf(from + chunk, original.size)
            val piece = original.copyOfRange(from, to)
            val payload = ByteArray(4 + piece.size)
            writeU32LE(payload, 0, jobId)
            System.arraycopy(piece, 0, payload, 4, piece.size)
            val enc = encryptEcb(key, payload)
            val fr = buildFrame(
                channelId = CHANNEL_FILE_ENC,
                interactive = INTERACTIVE_REQUEST,
                encoding = ENCODING_HEX,
                arcMsgSn = j + 1,
                msgSn = j + 1,
                encryptOffset = ENCRYPT_ECB_OFFSET,
                body = enc,
                pkgTotal = total,
                pkgNum = j + 1,
            )
            val dec = decryptEcb(key, parseFrame(fr).body)
            assertEquals(jobId, readU32LE(dec, 0))
            parts += dec.copyOfRange(4, 4 + piece.size)
        }
        val joined = parts.fold(ByteArray(0)) { acc, b -> acc + b }
        assertArrayEquals(original, joined)
    }

    @Test
    fun jobStateHelpers() {
        assertTrue(isActive("pending"))
        assertFalse(isTerminal("pending"))
        assertFalse(isNoPaperState("pending"))
        assertNull(detectJobFailure(mapOf("job_state" to "pending")))
        assertNull(detectNoPaperJobs(mapOf("result" to listOf(mapOf("job_state" to "pending")))))
        assertTrue(isSuccessState("finished"))
        assertTrue(isTerminal("canceled"))
        assertFalse(isSuccessState("canceled"))
        assertFalse(isNoPaperRpcError(mapOf("error" to mapOf("code" to -6002))))
        assertTrue(formatPrintJobError(mapOf("error" to mapOf("code" to -6002))).contains("-6002"))
        assertTrue(jobIdMatches(mapOf("job_id" to 47), 47))
    }
}
