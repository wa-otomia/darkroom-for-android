package io.github.wa_otomia.darkroom.core

import org.junit.Assert.*
import org.junit.Test

class ProPrinterStatusTest {
    @Test fun batteryIsNeverConvertedFromBandsOrUsb() {
        for (raw in 0..21) {
            val status = ProPrinterStatus(mapOf("battery" to raw, "sensor" to 12))
            assertNull(status.reportedBatteryPercent)
            assertEquals(true, status.usbConnected)
            assertEquals(true, status.coverClosed)
            assertEquals(when (raw) { in 0..5 -> false; in 16..21 -> true; else -> null }, status.charging)
        }
    }
    @Test fun reportedPercentUsesOnlyValidNumericFieldWithoutScaling() {
        for (v in listOf(null, "94", true, -1, 101, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(ProPrinterStatus(mapOf("battery-level" to v)).reportedBatteryPercent)
        }
        for (v in listOf(0, 94, 96, 100)) {
            assertEquals(v.toDouble(), ProPrinterStatus(mapOf("battery-level" to v)).reportedBatteryPercent)
        }
        assertEquals(94.5, ProPrinterStatus(mapOf("battery-level" to 94.5)).reportedBatteryPercent)
    }
    @Test fun exactProFaultPolicies() {
        for (code in listOf(-7103, -7110, -7111, -7112, -7114, -7201, -7208)) {
            assertTrue(ProPrinterStatus(mapOf("category" to "error", "error" to code)).canResume)
            assertFalse(ProPrinterStatus(mapOf("category" to "idle", "error" to code)).canResume)
            assertFalse(ProPrinterStatus(mapOf("category" to "error", "error" to code, "sensor" to 8)).canResume)
        }
        for (code in listOf(null, 0, -7101, -7203, -7001, -7104, -7105, -7204, -7205, -7308, -7309, -7310, -7311, -9999)) {
            val s = ProPrinterStatus(mapOf("category" to "error", "error" to code))
            assertTrue(s.hasFault)
            assertFalse(s.canResume)
        }
    }
    @Test fun readyRequiresIdleWithoutAnotherJobJustLikeOfficialQueue() {
        assertTrue(ProPrinterStatus(mapOf("category" to "idle")).readyForNewJob)
        for (s in listOf(emptyMap(), mapOf("category" to "processing"), mapOf("category" to "sleep"),
            mapOf("category" to "idle", "job_id" to 37), mapOf("category" to "idle", "job_id" to "bad"))) {
            assertFalse(ProPrinterStatus(s).readyForNewJob)
        }
    }
    @Test fun mixedResultMustBeObjectAndRpcErrorIsSeparate() {
        val raw = mapOf<String, Any?>("category" to "error", "error" to -7208)
        assertEquals(-7208, ProPrinterStatus.fromResponse(mapOf("result" to raw)).errorCode)
        assertFalse(hasRpcError(mapOf("result" to raw)))
        assertTrue(hasRpcError(mapOf("error" to mapOf("code" to -6002))))
        assertFalse(hasRpcError(mapOf("error" to mapOf("code" to 0))))
        assertTrue(hasRpcError(mapOf("error" to "bad")))
        assertThrows(IllegalStateException::class.java) { ProPrinterStatus.fromResponse(mapOf("result" to listOf(raw))) }
    }
    @Test fun sourceConstructorArrayParamsAndJsonEscaping() {
        assertEquals("{\"method\":\"job_info\",\"id\":7,\"params\":[37]}", encodeRpc("job_info",7,listOf(37)).toString(Charsets.UTF_8))
        assertEquals("{\"method\":\"resume_printer\",\"id\":8,\"params\":{}}", encodeRpc("resume_printer",8,emptyMap<String, Any>()).toString(Charsets.UTF_8))
        assertTrue(encodeRpc("m",1,listOf("\"\n\\")).toString(Charsets.UTF_8).contains("\\\"\\n\\\\"))
    }
    @Test fun terminalStatesAreNotGuessedFromProgressOrPending() {
        for (s in listOf("pending", "printing_Y", "printing_M", "printing_C", "printing_OC", "home_feed", "cool_down", "new_state", "", "done", "success")) {
            assertFalse(isTerminal(s)); assertFalse(isSuccessState(s)); assertNull(detectJobFailure(mapOf("job_state" to s)))
        }
        assertTrue(isSuccessState("finished")); assertTrue(isTerminal("canceled")); assertFalse(isSuccessState("canceled"))
    }
    @Test fun invalidFrameIsRejectedWithoutPaddingTruncatedBody() {
        val good = buildHello()
        assertThrows(IllegalArgumentException::class.java) { parseFrame(good.copyOf().also { it[20] = 0 }) }
        assertThrows(IllegalStateException::class.java) { parseFrame(good.copyOf(20)) }
        assertThrows(IllegalArgumentException::class.java) { buildFrame(3,6,3,1,1,5120,ByteArray(1024)) }
    }
    @Test fun idlessEventsAndLateRepliesCannotFulfillCurrentRequest() {
        val frame = parseFrame(buildFrame(3,7,3,12,12,0,byteArrayOf()))
        assertTrue(isRpcReply(frame,mapOf("id" to 12,"result" to listOf(1)),12))
        assertFalse(isRpcReply(frame,mapOf("result" to listOf(1)),12))
        assertFalse(isRpcReply(frame,mapOf("id" to 11,"result" to listOf(1)),12))
        assertFalse(isRpcReply(frame,mapOf("id" to 12,"method" to "event.rpt_err","params" to mapOf("code" to -8201)),12))
        assertFalse(isRpcReply(frame.copy(interactive=6),mapOf("id" to 12,"result" to listOf(1)),12))
    }
}
