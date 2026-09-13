package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException

class ProPrinterSessionTest {
    private val key = ByteArray(16) { it.toByte() }
    private class Clock { var time = 0L; fun sleep(ms: Long) { time += ms.coerceAtLeast(1) } }
    private inner class ScriptPipe : BytePipe {
        val commands = mutableListOf<Map<String, Any?>>()
        val inbox = ArrayDeque<Byte>()
        var dataFrames = 0
        var onData: () -> Unit = {}
        var handler: (String, Any?) -> Map<String, Any?>? = { method, _ ->
            when (method) {
                "print_job" -> mapOf("result" to mapOf("job_id" to 37))
                "mixed_status" -> mapOf("result" to mapOf("category" to "idle", "battery-level" to 94, "battery" to 4))
                "job_info" -> mapOf("result" to listOf(mapOf("job_id" to 37, "job_state" to "finished")))
                else -> mapOf("result" to listOf("ok"))
            }
        }
        fun response(message: Map<String, Any?>, interactive: Int = 7) {
            val id = (message["id"] as? Number)?.toInt() ?: 0
            val body = encryptEcb(key, JSONObject(message).toString().toByteArray())
            inbox.addAll(buildFrame(3, interactive, 3, id, id, 5120, body).toList())
        }
        override fun write(data: ByteArray) {
            var pos = 0
            while (pos < data.size) {
                val size = 22 + bodyLengthFromAttr(readU16LE(data, pos + 18))
                val frame = parseFrame(data.copyOfRange(pos, pos + size))
                pos += size
                if (frame.channelId == 4) {
                    dataFrames++
                    assertEquals(37, readU32LE(decryptEcb(key, frame.body), 0))
                    onData()
                } else {
                    val request = XiaomiPrinter.parseLooseJson(String(decryptEcb(key, frame.body, true)))!!
                    commands += request
                    val method = request["method"] as String
                    handler(method, request["params"])?.let { response(it + ("id" to request["id"])) }
                }
            }
        }
        override fun readExact(n: Int, timeoutMs: Int): ByteArray {
            if (inbox.size < n) throw SocketTimeoutException("simulated timeout")
            return ByteArray(n) { inbox.removeFirst() }
        }
        override fun close() = Unit
    }
    private fun make(pipe: ScriptPipe, clock: Clock = Clock(), status: (ProPrinterStatus?, String?) -> Unit = { _, _ -> }) =
        XiaomiPrinter(pipe, log = {}, onStatus = status, nowMs = { clock.time }, sleep = clock::sleep).also { it.useSessionKey(key) }
    private fun job(state: String) = mapOf("result" to listOf(mapOf("job_id" to 37, "job_state" to state)))
    private fun fault(code: Int, jobId: Int = 37) = mapOf("result" to mapOf("category" to "error", "error" to code, "job_id" to jobId, "sensor" to 12))

    @Test fun statusAndTaskCommandsUseOfficialShapes() {
        val pipe = ScriptPipe(); val p = make(pipe)
        assertEquals(94.0, p.mixedStatus().reportedBatteryPercent)
        p.jobInfo(37); p.deviceInfo(); p.cancelJob(37)
        assertEquals(emptyMap<String, Any>(), pipe.commands[0]["params"])
        assertEquals(listOf(37), pipe.commands[1]["params"])
        assertEquals(listOf("device_info"), pipe.commands[2]["params"])
        assertEquals(listOf(37), pipe.commands[3]["params"])
    }
    @Test fun jobIdIsPublishedBeforeAnyFileData() {
        val pipe = ScriptPipe(); val p = make(pipe)
        var callback = false
        p.printJpeg(ByteArray(3000), onJobCreated = {
            callback = true; assertEquals(37, it); assertEquals(0, pipe.dataFrames); assertEquals(37, p.currentJobId)
        })
        assertTrue(callback); assertEquals(4, pipe.dataFrames)
    }
    @Test fun unsolicitedAndIdlessAndLateMessagesDoNotStealAnRpc() {
        val pipe = ScriptPipe(); val p = make(pipe)
        pipe.response(mapOf("method" to "event.rpt_err", "params" to mapOf("code" to -8201)))
        pipe.response(mapOf("result" to mapOf("battery-level" to 0)))
        pipe.response(mapOf("id" to 99, "result" to "late"))
        assertEquals(94.0, p.mixedStatus().reportedBatteryPercent)
        assertEquals(3, p.diagnosticMessages().size)
    }
    @Test fun cancellationBeforeCreationIsLocalOnly() {
        val pipe = ScriptPipe(); val p = make(pipe)
        assertTrue(p.requestCancel())
        assertThrows(PrinterCancelled::class.java) { p.printJpeg(ByteArray(100)) }
        assertTrue(pipe.commands.isEmpty()); assertEquals(0, pipe.dataFrames)
    }
    @Test fun cancelDuringUploadUsesActualIdAndStopsLaterBatches() {
        val pipe = ScriptPipe(); val p = make(pipe)
        val fallback = pipe.handler
        pipe.handler = { m, params -> if (m == "job_info") job("canceled") else fallback(m, params) }
        pipe.onData = { p.requestCancel(37) }
        assertThrows(PrinterCancelled::class.java) { p.printJpeg(ByteArray(CHUNK_BYTES * 10)) }
        assertEquals(3, pipe.dataFrames) // Complete the already-written batch, not a partial frame.
        assertEquals(1, pipe.commands.count { it["method"] == "cancel_job" })
        assertEquals(listOf(37), pipe.commands.first { it["method"] == "cancel_job" }["params"])
        assertTrue(p.remoteJobSettled)
    }
    @Test fun cancelImmediatelyAfterCreationSendsNoPhotoBytes() {
        val pipe = ScriptPipe(); val p = make(pipe); val fallback = pipe.handler
        pipe.handler = { m, a -> if (m == "job_info") job("canceled") else fallback(m, a) }
        assertThrows(PrinterCancelled::class.java) {
            p.printJpeg(ByteArray(100), onJobCreated = { p.requestCancel(it) })
        }
        assertEquals(0, pipe.dataFrames)
    }
    @Test fun lostCancelAcknowledgementIsReconciledWithoutReplaying() {
        val pipe = ScriptPipe(); val p = make(pipe); val fallback = pipe.handler
        pipe.handler = { m, a -> when (m) { "cancel_job" -> null; "job_info" -> job("canceled"); else -> fallback(m, a) } }
        p.printJpeg(ByteArray(20)); p.requestCancel(37)
        assertThrows(PrinterCancelled::class.java) { p.waitUntilDone(37) }
        assertEquals(1, pipe.commands.count { it["method"] == "cancel_job" })
    }
    @Test fun cancelAckIsNotTerminalAndTimeoutNeverReportsCanceled() {
        val pipe = ScriptPipe(); val p = make(pipe); val fallback = pipe.handler
        pipe.handler = { m, a -> if (m == "job_info") job("printing_Y") else fallback(m, a) }
        p.printJpeg(ByteArray(20)); p.requestCancel(37)
        assertThrows(PrinterOutcomeUnknown::class.java) { p.waitUntilDone(37) }
        assertFalse(p.remoteJobSettled)
        assertEquals(1, pipe.commands.count { it["method"] == "cancel_job" })
    }
    @Test fun rejectedCancelIsFailureNotSuccessfulCancellation() {
        val pipe = ScriptPipe(); val p = make(pipe); val fallback = pipe.handler
        pipe.handler = { m, a -> if (m == "cancel_job") mapOf("error" to mapOf("code" to -6002)) else fallback(m, a) }
        p.printJpeg(ByteArray(20)); p.requestCancel(37)
        assertThrows(PrinterError::class.java) { p.waitUntilDone(37) }
        assertFalse(p.remoteJobSettled)
    }
    @Test fun finishedRacingWithCancelRemainsSuccessful() {
        val pipe = ScriptPipe(); val p = make(pipe)
        p.printJpeg(ByteArray(20)); p.requestCancel(37)
        assertEquals("finished", p.waitUntilDone(37)["job_state"])
        assertTrue(p.remoteJobSettled)
    }
    @Test fun wrongJobCannotBeCanceledThroughCurrentSession() {
        val pipe = ScriptPipe(); val p = make(pipe)
        p.printJpeg(ByteArray(20)); assertFalse(p.requestCancel(99))
        assertEquals("finished", p.waitUntilDone(37)["job_state"])
        assertFalse(pipe.commands.any { it["method"] == "cancel_job" })
    }
    @Test fun handledPaperFaultResumesOriginalJobWithoutNewPrintRequest() {
        val pipe = ScriptPipe(); var resumed = false; var polls = 0
        lateinit var p: XiaomiPrinter
        p = make(pipe, status = { _, phase -> if (phase == "waiting_for_user") p.requestResume() })
        val fallback = pipe.handler
        pipe.handler = { m, a -> when (m) {
            "job_info" -> job(if (resumed && ++polls > 1) "finished" else "pending")
            "mixed_status" -> if (!resumed) fault(-7103) else mapOf("result" to mapOf("category" to "processing", "job_id" to 37))
            "resume_printer" -> { resumed = true; assertEquals(emptyMap<String, Any>(), a); mapOf("result" to listOf("ok")) }
            else -> fallback(m, a)
        } }
        p.printJpeg(ByteArray(20))
        assertEquals("finished", p.waitUntilDone(37)["job_state"])
        assertEquals(1, pipe.commands.count { it["method"] == "resume_printer" })
        assertEquals(1, pipe.commands.count { it["method"] == "print_job" })
    }
    @Test fun unknownFaultAndOtherJobNeverSendResume() {
        for ((code, remote) in listOf(-9999 to 37, -7208 to 99)) {
            val pipe = ScriptPipe(); lateinit var p: XiaomiPrinter
            p = make(pipe, status = { _, phase -> if (phase == "waiting_for_user") p.requestResume() })
            val fallback = pipe.handler
            var polls = 0
            pipe.handler = { m, a -> when (m) {
                "job_info" -> job(if (++polls > 2) "finished" else "pending")
                "mixed_status" -> fault(code, remote)
                else -> fallback(m, a)
            } }
            p.printJpeg(ByteArray(20)); p.waitUntilDone(37)
            assertFalse(pipe.commands.any { it["method"] == "resume_printer" })
        }
    }
    @Test fun repeatedClicksDoNotRepeatUnconfirmedResume() {
        val pipe = ScriptPipe(); lateinit var p: XiaomiPrinter
        p = make(pipe, status = { _, _ -> repeat(3) { p.requestResume() } })
        val fallback = pipe.handler
        pipe.handler = { m, a -> when(m) { "job_info" -> job("pending"); "mixed_status" -> fault(-7208); else -> fallback(m,a) } }
        p.printJpeg(ByteArray(20))
        assertThrows(PrinterOutcomeUnknown::class.java) { p.waitUntilDone(37) }
        assertEquals(1, pipe.commands.count { it["method"] == "resume_printer" })
    }
    @Test fun paperAndRibbonCodesDoNotComeFromRpcSystemError() {
        val pipe = ScriptPipe(); val p = make(pipe)
        pipe.handler = { _, _ -> mapOf("error" to mapOf("code" to -6002)) }
        val e = assertThrows(PrinterError::class.java) { p.mixedStatus() }
        assertFalse(e is NoPaperError); assertNull(p.lastStatus)
    }
    @Test fun missingOrUnknownTaskResultNeverSucceeds() {
        for (result in listOf(emptyList<Any>(), listOf(mapOf("job_id" to 37, "job_state" to "new_state")))) {
            val pipe = ScriptPipe(); val p = make(pipe); val fallback = pipe.handler
            pipe.handler = { m, a -> if(m == "job_info") mapOf("result" to result) else fallback(m,a) }
            p.printJpeg(ByteArray(20))
            assertThrows(PrinterOutcomeUnknown::class.java) { p.waitUntilDone(37, timeoutMs=3_000) }
            assertFalse(p.remoteJobSettled)
        }
    }
    @Test fun checksumFailureResynchronizesAtNextValidFrame() {
        val pipe = ScriptPipe(); val p = make(pipe)
        pipe.inbox.addAll(buildHello().also { it[it.size-2] = 0 }.toList())
        assertEquals(94.0, p.mixedStatus().reportedBatteryPercent)
    }
    @Test fun explicitlyRejectedCreationIsNotAnUnknownAcceptedJob() {
        val pipe = ScriptPipe(); val p = make(pipe)
        pipe.handler = { _, _ -> mapOf("error" to mapOf("code" to -6002)) }
        assertThrows(PrinterError::class.java) { p.printJpeg(ByteArray(20)) }
        assertTrue(p.jobCreationAttempted)
        assertTrue(p.remoteJobSettled)
        assertNull(p.currentJobId)
        assertEquals(0, pipe.dataFrames)
    }
    @Test fun malformedJobReferenceCannotEnableResume() {
        val pipe = ScriptPipe(); lateinit var p: XiaomiPrinter
        p = make(pipe, status = { _, phase -> if (phase == "waiting_for_user") assertFalse(p.requestResume()) })
        val fallback = pipe.handler
        var polls = 0
        pipe.handler = { m, a -> when (m) {
            "job_info" -> job(if (++polls > 2) "finished" else "pending")
            "mixed_status" -> mapOf("result" to mapOf("category" to "error", "error" to -7208, "job_id" to "bad"))
            else -> fallback(m, a)
        } }
        p.printJpeg(ByteArray(20)); p.waitUntilDone(37)
        assertFalse(pipe.commands.any { it["method"] == "resume_printer" })
    }

}
