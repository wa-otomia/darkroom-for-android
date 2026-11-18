package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** EDID040-R09: the per-version record of a failed cut-out. */
class IdMaskFailureRecordTest {
    private val reply = byteArrayOf(1, 2, 3, 4)

    @Test
    fun anyReceivedReplyIsKeptWhateverTheFailure() {
        // Undecodable, wrong aspect, not a matte, version gone, encode failure: the bytes that
        // arrived are kept as they are.
        for (m in listOf(
            ID_MASK_MSG_UNDECODABLE,
            ID_MASK_MSG_ASPECT,
            ID_MASK_MSG_NOT_BINARY,
            ID_MASK_MSG_NO_PERSON,
            ID_MASK_MSG_NO_BACKGROUND,
            ID_MASK_MSG_VERSION_GONE,
            "PNG 编码失败",
        )) {
            val record = idMaskFailureRecord(m, reply, at = 1L)
            assertTrue(m, record.hasImage)
            assertEquals(m, record.message)
        }
        assertArrayEquals(reply, idMaskFailureReply(reply))
    }

    @Test
    fun failuresWithoutAReplyStillLeaveARecord() {
        for (m in listOf(ID_MASK_MSG_NEED_KEY, "没有返回图片", "无法下载返回的图片", "timeout", "HTTP 400")) {
            val record = idMaskFailureRecord(m, null, at = 5L)
            assertFalse(m, record.hasImage)
            assertEquals(m, record.message)
            assertEquals(5L, record.at)
        }
        // An empty body is no reply.
        assertNull(idMaskFailureReply(ByteArray(0)))
        assertFalse(idMaskFailureRecord("x", ByteArray(0), 0L).hasImage)
        assertNull(idMaskFailureReply(null))
    }

    @Test
    fun messageIsSanitizedTrimmedAndCut() {
        val secret = "Authorization: Bearer xai-ABCDEFGHIJKLMNOP failed"
        val clean = idMaskFailureMessage("  $secret \n")
        assertFalse(clean.contains("xai-ABCDEFGHIJKLMNOP"))
        assertFalse(clean.startsWith(" "))
        assertEquals("", idMaskFailureMessage(null))

        val long = "a".repeat(ID_MASK_FAILURE_MESSAGE_MAX + 100)
        assertEquals(ID_MASK_FAILURE_MESSAGE_MAX, idMaskFailureMessage(long).length)
        // A surrogate pair straddling the cut is dropped whole.
        val emoji = "a".repeat(ID_MASK_FAILURE_MESSAGE_MAX - 1) + "😀" + "tail"
        val cut = idMaskFailureMessage(emoji)
        assertEquals(ID_MASK_FAILURE_MESSAGE_MAX - 1, cut.length)
        assertFalse(Character.isHighSurrogate(cut.last()))
    }

    @Test
    fun userCancelNeverEndsAsAFailure() {
        // A cancellation always ends as a cancel.
        assertTrue(idMaskEndsAsCancel(kotlin.coroutines.cancellation.CancellationException("x"), jobActive = true))
        assertTrue(idMaskEndsAsCancel(kotlin.coroutines.cancellation.CancellationException("x"), jobActive = false))
        // A cancel that lands while the reply is decoded surfaces as the decode's own error:
        // the cancelled job still ends as a cancel, with no failure record.
        for (m in listOf(ID_MASK_MSG_UNDECODABLE, ID_MASK_MSG_ASPECT, ID_MASK_MSG_NOT_BINARY, "HTTP 500")) {
            assertTrue(m, idMaskEndsAsCancel(IllegalStateException(m), jobActive = false))
            // The same error in a job nobody cancelled is a failure.
            assertFalse(m, idMaskEndsAsCancel(IllegalStateException(m), jobActive = true))
        }
    }

    @Test
    fun recordRoundTripsWithStableKeys() {
        val record = IdMaskFailureRecord(ID_MASK_MSG_NOT_BINARY, 1_763_000_000_000L, hasImage = true)
        val raw = encodeIdMaskFailureRecord(record)
        assertTrue(raw, raw.contains("\"message\""))
        assertTrue(raw, raw.contains("\"at\":1763000000000"))
        assertTrue(raw, raw.contains("\"hasImage\":true"))
        assertEquals(record, parseIdMaskFailureRecord(raw))
        // Unknown keys from a later version are ignored.
        assertEquals(
            IdMaskFailureRecord("x", 2L, false),
            parseIdMaskFailureRecord("""{"message":"x","at":2,"hasImage":false,"extra":1}"""),
        )
    }

    @Test
    fun missingOrCorruptRecordIsNone() {
        assertNull(parseIdMaskFailureRecord(null))
        assertNull(parseIdMaskFailureRecord(""))
        assertNull(parseIdMaskFailureRecord("not json"))
        assertNull(parseIdMaskFailureRecord("""{"message":"x","at":2"""))
        assertNull(parseIdMaskFailureRecord("""{"message":"x","hasImage":true}"""))
        assertNull(parseIdMaskFailureRecord("""{"at":2,"hasImage":true}"""))
        assertNull(parseIdMaskFailureRecord("""["x",2,true]"""))
    }

    @Test
    fun parsedMessageIsCleanedAgain() {
        val raw = """{"message":"  key sk-ABCDEFGHIJKLMNOP  ","at":3,"hasImage":false}"""
        val record = parseIdMaskFailureRecord(raw)!!
        assertFalse(record.message.contains("sk-ABCDEFGHIJKLMNOP"))
        assertFalse(record.message.startsWith(" "))
    }
}
