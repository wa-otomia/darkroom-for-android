package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.*
import java.io.*
import java.math.BigInteger
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// Dependency-free checks against production classes, NOT a substitute for the
// Android/Compose build or the org.json/JUnit printer-session suite.
private var passed = 0
internal fun verify(name: String, block: () -> Unit) {
    try { block(); passed++; println("PASS $name") }
    catch (t: Throwable) { System.err.println("FAIL $name: $t"); throw t }
}
internal fun hex(s: String): ByteArray = s.filterNot(Char::isWhitespace).chunked(2).map { it.toInt(16).toByte() }.toByteArray()
private inline fun <reified E : Throwable> rejects(block: () -> Unit) {
    try { block() } catch (t: Throwable) { check(t is E) { "Expected ${E::class.java.name}, got $t" }; return }
    error("Expected ${E::class.java.name}")
}
private class MemoryPipe : BytePipe {
    val bytes = ArrayDeque<Byte>()
    fun feed(b: ByteArray) { bytes.addAll(b.toList()) }
    override fun write(data: ByteArray) = error("Read-only fixture")
    override fun readExact(n: Int, timeoutMs: Int): ByteArray {
        if (bytes.size < n) throw SocketTimeoutException("Fixture incomplete")
        return ByteArray(n) { bytes.removeFirst() }
    }
    override fun close() = Unit
}

fun main() {
    verify("hello/captured-wire-vector") {
        check(toHex(buildHello()) == CAPTURED_HELLO_HEX)
    }
    verify("AES/known-answer") {
        check(encryptEcb(hex("000102030405060708090a0b0c0d0e0f"), hex("00112233445566778899aabbccddeeff"))
            .contentEquals(hex("69c4e0d86a7b0430d8cdb78070b4c55a")))
    }
    verify("AES/zero-padding-all-lengths") {
        val key = ByteArray(16) { it.toByte() }
        for (n in 0..992) {
            val p = ByteArray(n) { ((it % 254) + 1).toByte() }
            val e = encryptEcb(key, p)
            check(e.size == ((n + 15) / 16) * 16)
            check(decryptEcb(key, e, true).contentEquals(p))
        }
    }
    verify("AES/binary-trailing-zero-retained") {
        val key = ByteArray(16)
        check(decryptEcb(key, encryptEcb(key, byteArrayOf(7, 0))).take(2) == listOf(7.toByte(), 0.toByte()))
    }
    verify("DH/mutual-session-and-confirm") {
        val p = BigInteger("FFFFFFFFFFFFFFC5", 16)
        val g = BigInteger.TWO
        val a = BigInteger.valueOf(123456789)
        val ra = modPow(g, a, p)
        val pField = intToBytes(p)
        val hello = byteArrayOf(0x32, 0, 0, 0) + pField + pack16Front(intToBytes(ra))
        val client = deriveSession(hello, BigInteger.valueOf(987654321))
        val serverKey = pack16End(intToBytes(modPow(bytesToInt(client.rbField), a, p)))
        check(serverKey.contentEquals(client.key))
        check(decryptEcb(serverKey, client.encP).contentEquals(pField))
    }
    verify("RPC/array-shapes-and-escaping") {
        check(String(encodeRpc("job_info", 9, listOf(37))) == "{\"method\":\"job_info\",\"id\":9,\"params\":[37]}")
        check(String(encodeRpc("resume_printer", 9, emptyMap<String, Any>())) == "{\"method\":\"resume_printer\",\"id\":9,\"params\":{}}")
        check(String(encodeRpc("x", 1, listOf("\"\n\\\u0001"))).contains("\\\"\\n\\\\\\u0001"))
    }
    verify("RPC/reject-unsupported-or-nonfinite-params") {
        rejects<IllegalArgumentException> { encodeRpc("x", 1, "wrong") }
        rejects<IllegalArgumentException> { encodeRpc("x", 1, listOf(Double.NaN)) }
        rejects<IllegalArgumentException> { encodeRpc("x", 1, mapOf(3 to 7)) }
    }
    verify("RPC/Android-origin-is-64-not-frame-channel") {
        check(printJobParams(2000, 2) == mapOf("file_size" to 2000, "copies" to 2, "job_type" to 0, "channel" to 64))
        check(CHANNEL_DATA_ENC == 3 && CHANNEL_FILE_ENC == 4)
    }
    verify("frame/all-supported-body-lengths") {
        for (n in 0..1023) {
            val body = ByteArray(n) { it.toByte() }
            val raw = buildFrame(3, 7, 3, 123, 124, 5120, body)
            check(verifyChecksum(raw))
            val f = parseFrame(raw)
            check(f.arcMsgSn == 123 && f.msgSn == 124 && f.body.contentEquals(body))
        }
    }
    verify("frame/corruption-rejected") {
        rejects<IllegalArgumentException> { parseFrame(buildHello().also { it[20] = 0 }) }
        rejects<IllegalArgumentException> { parseFrame(buildHello().also { it[1] = 0 }) }
        rejects<IllegalStateException> { parseFrame(buildHello().copyOf(20)) }
        rejects<IllegalStateException> { parseFrame(buildHello().also { it[it.lastIndex] = 0 }) }
        rejects<IllegalArgumentException> { buildFrame(3, 6, 3, 1, 1, 5120, ByteArray(1024)) }
    }
    verify("stream/byte-fragmented-and-coalesced-frames") {
        val pipe = MemoryPipe(); val reader = PrinterFrameReader(pipe)
        val raw = buildHello()
        for (i in 0 until raw.lastIndex) {
            pipe.feed(byteArrayOf(raw[i]))
            rejects<SocketTimeoutException> { reader.read(100) }
        }
        pipe.feed(byteArrayOf(raw.last()) + raw)
        check(String(reader.read(100).body) == "hello")
        check(String(reader.read(100).body) == "hello")
    }
    verify("stream/ciphertext-like-delimiters-not-split") {
        val pipe = MemoryPipe(); val reader = PrinterFrameReader(pipe)
        val body = byteArrayOf(0x7e, 0x64, 0, 3, 6, 0x7e, 0x7e)
        pipe.feed(buildFrame(3, 7, 3, 1, 1, 0, body))
        check(reader.read(100).body.contentEquals(body))
    }
    verify("stream/resync-after-noise-and-bad-checksum") {
        val pipe = MemoryPipe(); val reader = PrinterFrameReader(pipe)
        val bad = buildHello().also { it[it.size - 2] = 0 }
        pipe.feed(byteArrayOf(1, 2, 3, 0x7e, 1) + bad + buildHello(7))
        check(reader.read(100).msgSn == 7)
    }
    verify("battery/bands-never-become-percent") {
        for (v in 0..21) {
            val s = ProPrinterStatus(mapOf("battery" to v, "sensor" to 12))
            check(s.reportedBatteryPercent == null)
            check(s.usbConnected == true && s.coverClosed == true)
            check(s.charging == when (v) { in 0..5 -> false; in 16..21 -> true; else -> null })
        }
    }
    verify("battery/reported-percent-is-not-scaled") {
        for (v in listOf(0, 94, 96, 100)) check(ProPrinterStatus(mapOf("battery-level" to v)).reportedBatteryPercent == v.toDouble())
        check(ProPrinterStatus(mapOf("battery-level" to 94.5)).reportedBatteryPercent == 94.5)
    }
    verify("battery/invalid-percent-stays-unknown") {
        for (v in listOf(null, "94", false, -1, 101, Double.NaN, Double.POSITIVE_INFINITY)) {
            check(ProPrinterStatus(mapOf("battery-level" to v)).reportedBatteryPercent == null)
        }
    }
    verify("sensors/unknown-and-USB-not-charging") {
        check(ProPrinterStatus(emptyMap()).usbConnected == null)
        val s = ProPrinterStatus(mapOf("battery" to 4, "sensor" to 8))
        check(s.usbConnected == true && s.coverClosed == false && s.charging == false)
    }
    verify("fault/seven-official-recoverable-codes") {
        for (code in listOf(-7103, -7110, -7111, -7112, -7114, -7201, -7208)) {
            check(ProPrinterStatus(mapOf("category" to "error", "error" to code)).canResume)
            check(!ProPrinterStatus(mapOf("category" to "idle", "error" to code)).canResume)
            check(!ProPrinterStatus(mapOf("category" to "error", "error" to code, "sensor" to 8)).canResume)
        }
    }
    verify("fault/unknown-and-nonrecoverable-codes") {
        for (code in listOf(null, 0, -7101, -7203, -7001, -7104, -7105, -7204, -7205, -7308, -7309, -7310, -7311, -9999)) {
            val s = ProPrinterStatus(mapOf("category" to "error", "error" to code))
            check(s.hasFault && !s.canResume)
        }
    }
    verify("fault/result-error-is-not-RPC-error") {
        val raw = mapOf("category" to "error", "error" to -7208)
        val res = mapOf("result" to raw)
        check(!hasRpcError(res))
        check(ProPrinterStatus.fromResponse(res).errorCode == -7208)
        check(hasRpcError(mapOf("error" to mapOf("code" to -6002))))
        check(!hasRpcError(mapOf("error" to mapOf("code" to 0))))
        check(!isNoPaperRpcError(mapOf("error" to mapOf("code" to -6002))))
    }
    verify("status/result-must-be-object") {
        rejects<IllegalStateException> { ProPrinterStatus.fromResponse(mapOf("result" to listOf(1))) }
        rejects<IllegalArgumentException> { ProPrinterStatus.fromResponse(mapOf("error" to -6002)) }
    }
    verify("ready/idle-without-any-job-reference-only") {
        check(ProPrinterStatus(mapOf("category" to "idle")).readyForNewJob)
        for (raw in listOf(emptyMap(), mapOf("category" to "processing"), mapOf("category" to "sleep"),
            mapOf("category" to "idle", "job_id" to 0), mapOf("category" to "idle", "job_id" to "bad"))) {
            check(!ProPrinterStatus(raw).readyForNewJob)
        }
    }
    verify("routing/only-exact-id-reply-can-complete-request") {
        val f = parseFrame(buildFrame(3, 7, 3, 12, 12, 0, byteArrayOf()))
        check(isRpcReply(f, mapOf("id" to 12, "result" to 1), 12))
        for (r in listOf(mapOf("result" to 1), mapOf("id" to 13, "result" to 1),
            mapOf("id" to 12, "method" to "event.rpt_err", "params" to 1), mapOf("id" to "12", "result" to 1))) {
            check(!isRpcReply(f, r, 12))
        }
        check(!isRpcReply(f.copy(interactive = 6), mapOf("id" to 12, "result" to 1), 12))
    }
    verify("job/only-finished-is-success") {
        check(isSuccessState("finished"))
        check(isTerminal("canceled") && !isSuccessState("canceled"))
        check(isTerminal("aborted") && !isSuccessState("aborted"))
        for (s in listOf("pending", "printing_Y", "printing_M", "printing_C", "printing_OC", "home_feed", "cool_down", "done", "success", "new_firmware_state", "")) {
            check(!isSuccessState(s) && !isTerminal(s))
        }
    }
    verify("restore/remote-work-never-auto-requeues") {
        for (s in listOf("waiting_for_user", "resuming", "canceling", "outcome_unknown", "sending", "printing")) {
            check(restoreRunningPrintJob(s) == PrintRestoreAction.FailInterrupted)
        }
        check(restoreRunningPrintJob(null, true) == PrintRestoreAction.FailInterrupted)
        check(restoreRunningPrintJob("preparing") == PrintRestoreAction.Requeue)
    }
    verify("cancel/intent-is-not-remote-confirmation") {
        check(mapPrintWorkerFailure(false, true, "IO error", true).state == "failed")
        check(mapPrintWorkerFailure(true, true, "worker stopped", true).state == "failed")
        check(mapPrintWorkerFailure(false, true, null, true, true).state == "cancelled")
        check(mapPrintWorkerFailure(true, true, "local", false).state == "cancelled")
    }
    verify("cancel/flags-are-per-local-job") {
        val flags = PrintJobCancelFlags()
        flags.request("A"); flags.clear("B")
        check(flags.isRequested("A") && !flags.isRequested("B"))
        flags.clear("A"); check(!flags.isRequested("A"))
    }
    verify("IO/read-timeout-preserves-partial-bytes") {
        val input = PipedInputStream(); val sender = PipedOutputStream(input)
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close(); sender.close() }
        try {
            sender.write(0x7e); sender.flush()
            rejects<SocketTimeoutException> { pipe.readExact(2, 30) }
            sender.write(0x64); sender.flush()
            check(pipe.readExact(2, 1000).contentEquals(byteArrayOf(0x7e, 0x64)))
        } finally { pipe.close() }
    }
    verify("IO/EOF-is-bounded") {
        val input = ByteArrayInputStream(byteArrayOf(1))
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close() }
        try {
            check(pipe.readExact(1, 1000).contentEquals(byteArrayOf(1)))
            rejects<EOFException> { pipe.readExact(1, 1000) }
        } finally { pipe.close() }
    }
    verify("IO/close-unblocks-reader") {
        val input = PipedInputStream(); val sender = PipedOutputStream(input)
        val pipe = StreamBytePipe(input, ByteArrayOutputStream()) { input.close(); sender.close() }
        pipe.close()
        rejects<IOException> { pipe.readExact(1, 100) }
    }
    verify("IO/write-watchdog-closes-without-replay") {
        val closed = CountDownLatch(1)
        val input = object : InputStream() {
            override fun read(): Int { closed.await(2, TimeUnit.SECONDS); return -1 }
        }
        var writes = 0
        val output = object : OutputStream() {
            override fun write(b: Int) {
                writes++
                check(closed.await(2, TimeUnit.SECONDS)) { "write watchdog did not run" }
                throw IOException("Closed by watchdog")
            }
        }
        val pipe = StreamBytePipe(input, output, 30) { closed.countDown() }
        try {
            val start = System.nanoTime()
            rejects<IOException> { pipe.write(byteArrayOf(1)) }
            check(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 1500)
            check(writes == 1)
        } finally { pipe.close() }
    }
    verifyOfficialVectors()
    println("SUMMARY: $passed named dependency-free checks passed.")
    println("Scope excludes Android/Compose build, Room queue integration, org.json session tests and hardware.")
}
