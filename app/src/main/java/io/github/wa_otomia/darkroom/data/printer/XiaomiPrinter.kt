package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

open class PrinterError(message: String) : Exception(message)
class HandshakeError(message: String) : PrinterError(message)
class NoPaperError(message: String = NO_PAPER_MESSAGE) : PrinterError(message) {
    val code: String = "NO_PAPER"
}
/** Only local-before-creation or a device-reported canceled job produces this exception. */
class PrinterCancelled : PrinterError("\u5df2\u53d6\u6d88")
class PrinterOutcomeUnknown(message: String) : PrinterError(message)

data class SendStats(val bytes: Int, val frames: Int, val writes: Int, val elapsedMs: Long) {
    val kbPerSec: Double get() = if (elapsedMs <= 0L) 0.0 else bytes / 1024.0 / (elapsedMs / 1000.0)
    override fun toString(): String = "%d B in %d frames / %d writes, %d ms, %.1f KB/s"
        .format(bytes, frames, writes, elapsedMs, kbPerSec)
}

/** One synchronous IO owner. UI threads enqueue control intents; they never read the socket.
 * Wire contract: official plugin com.hannto.printer 1.1.15 (69), module 12407.
 * DH and image framing remain compatible with the existing working implementation. */
class XiaomiPrinter(
    private val pipe: BytePipe,
    private val readTimeout: Int = 8_000,
    private val interWritePaceMs: Long = 0L,
    private val framesPerWrite: Int = MAX_FRAMES_PER_WRITE,
    private val log: (String) -> Unit = { android.util.Log.i(LOG_TAG, it) },
    private val onStatus: (ProPrinterStatus?, String?) -> Unit = { _, _ -> },
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000 },
    private val sleep: (Long) -> Unit = { Thread.sleep(it) },
) {
    var key: ByteArray? = null
        private set
    internal fun useSessionKey(k: ByteArray) { key = k.copyOf() }
    private val reader = PrinterFrameReader(pipe)
    private var sn = 0
    private val cancelRequested = AtomicBoolean(false)
    private val resumeRequested = AtomicBoolean(false)
    @Volatile var currentJobId: Int? = null
        private set
    @Volatile var jobCreationAttempted = false
        private set
    @Volatile var remoteJobSettled = false
        private set
    @Volatile var lastStatus: ProPrinterStatus? = null
        private set
    @Volatile var controlPhase: String? = null
        private set
    @Volatile var lastSendStats: SendStats? = null
        private set
    private var cancelSent = false
    private var settledJob: Map<String, Any?>? = null
    private var resumedError: Int? = null
    private var resumeSentAt: Long? = null
    // Bounded diagnostic inbox: events/late replies must not fulfill unrelated RPCs.
    private val unclaimed = ArrayDeque<Map<String, Any?>>()

    init { require(framesPerWrite in 1..32 && readTimeout > 0 && interWritePaceMs >= 0) }

    fun requestCancel(jobId: Int? = null): Boolean {
        if (jobId != null && jobId != currentJobId) return false
        cancelRequested.set(true)
        return true
    }

    fun requestResume(): Boolean {
        val status = lastStatus ?: return false
        if (cancelRequested.get() || controlPhase != "waiting_for_user" || !canResumeSnapshot(status)) return false
        return resumeRequested.compareAndSet(false, true)
    }

    @Synchronized fun connect() { handshake() }
    fun disconnect() {
        try { pipe.close() } finally {
            key = null
            lastStatus = null
            onStatus(null, null)
        }
    }
    private fun nextSn(): Int {
        check(sn < Int.MAX_VALUE) { "Reconnect before sequence number exhaustion" }
        return ++sn
    }
    private fun readFrame(timeoutMs: Int = readTimeout): Frame = reader.read(timeoutMs)
    private fun readUntil(channelId: Int, interactive: Int, timeoutMs: Int = 10_000): Frame {
        val deadline = nowMs() + timeoutMs
        while (nowMs() < deadline) {
            val f = readFrame((deadline - nowMs()).toInt().coerceAtLeast(1))
            if (f.channelId == channelId && f.interactive == interactive) return f
        }
        throw SocketTimeoutException("Expected handshake frame not received")
    }

    private fun handshake(timeoutMs: Int = 12_000) {
        pipe.write(buildHello(nextSn()))
        val srv = readUntil(CHANNEL_AUTH, INTERACTIVE_SERVER_HELLO, timeoutMs)
        if (srv.body.size < 36) throw HandshakeError("bad server hello (len=${srv.body.size})")
        val info = deriveSession(srv.body.copyOf(36))
        key = info.key
        val sn2 = nextSn()
        pipe.write(
            buildFrame(
                channelId = CHANNEL_AUTH,
                interactive = INTERACTIVE_CLIENT_CONFIRM,
                encoding = ENCODING_JSON,
                arcMsgSn = sn2,
                msgSn = sn2,
                encryptOffset = ENCRYPT_ECB_OFFSET,
                body = info.rbField + info.encP,
            ),
        )
        val conf = readUntil(CHANNEL_AUTH, INTERACTIVE_SERVER_CONFIRM, timeoutMs)
        val msg = String(conf.body, Charsets.ISO_8859_1).trimEnd('\u0000').lowercase()
        if (!msg.startsWith("ok")) throw HandshakeError("printer rejected handshake: $msg")
    }


    @Synchronized
    fun command(method: String, params: Any, timeoutMs: Int = 15_000): Map<String, Any?> {
        val sessionKey = key ?: throw PrinterError("Printer is not connected")
        val id = nextSn()
        pipe.write(buildFrame(CHANNEL_DATA_ENC, INTERACTIVE_REQUEST, ENCODING_JSON,
            id, id, ENCRYPT_ECB_OFFSET, encryptEcb(sessionKey, encodeRpc(method, id, params))))
        val deadline = nowMs() + timeoutMs
        while (nowMs() < deadline) {
            val f = readFrame((deadline - nowMs()).toInt().coerceAtLeast(1))
            if (f.channelId != CHANNEL_DATA_ENC || f.encoding != ENCODING_JSON ||
                f.body.isEmpty() || f.body.size % 16 != 0 ||
                ((f.msgAttr ushr 10) and 7) != 5) continue
            val res = parseLooseJson(String(decryptEcb(sessionKey, f.body, stripZeros = true), Charsets.UTF_8)) ?: continue
            if (isRpcReply(f, res, id)) return res
            if (unclaimed.size == 32) unclaimed.removeFirst()
            unclaimed.add(res)
            // Do not log raw JSON, MAC addresses, keys or image data.
            log("Unclaimed printer message on channel ${f.channelId}, interactive ${f.interactive}")
        }
        throw SocketTimeoutException("No response to $method")
    }

    @Synchronized fun diagnosticMessages(): List<Map<String, Any?>> = unclaimed.toList()
    fun jobInfo(jobId: Int, timeoutMs: Int = 8_000): Map<String, Any?> {
        require(jobId >= 0)
        return command("job_info", listOf(jobId), timeoutMs)
    }
    fun deviceInfo(timeoutMs: Int = 8_000) = command("get_prop", listOf("device_info"), timeoutMs)

    @Synchronized fun mixedStatus(timeoutMs: Int = 8_000): ProPrinterStatus {
        try {
            val response = command("mixed_status", emptyMap<String, Any>(), timeoutMs)
            requireSuccess("mixed_status", response)
            val status = ProPrinterStatus.fromResponse(response)
            lastStatus = status
            onStatus(status, controlPhase)
            return status
        } catch (e: Exception) {
            lastStatus = null // Never expose a stale ready state/percentage as a fresh reading.
            onStatus(null, controlPhase)
            throw e
        }
    }

    /** Preflight never creates a job. Faults stay actionable; new work cannot bypass another job. */
    @Synchronized fun checkPaperReady(timeoutMs: Int = 8_000) {
        val status = mixedStatus(timeoutMs)
        if (status.hasFault) throw PrinterError("Printer fault ${status.errorCode ?: "unknown"}")
        if (!status.readyForNewJob) throw PrinterError("Printer is not ready: ${status.category ?: "unknown"}")
    }

    @Synchronized fun awaitReady(timeoutMs: Int = 900_000, pollMs: Long = 2_000) {
        val deadline = nowMs() + timeoutMs
        while (nowMs() < deadline) {
            if (cancelRequested.get()) throw PrinterCancelled()
            val status = mixedStatus()
            if (status.readyForNewJob) { phase(null); return }
            phase(if (resumeSentAt != null && status.errorCode == resumedError) "resuming" else "waiting_for_user")
            handleResume(status)
            sleep(pollMs)
        }
        throw PrinterError("Timed out waiting for printer readiness")
    }

    private fun phase(value: String?) {
        controlPhase = value
        onStatus(lastStatus, value)
    }

    private fun requireSuccess(method: String, res: Map<String, Any?>) {
        if (hasRpcError(res)) throw PrinterError("$method rejected (RPC ${rpcErrorCode(res) ?: "unknown"})")
        if (!res.containsKey("result")) throw PrinterError("$method has no result")
    }

    /** A response acknowledges the command, NOT the eventual canceled/finished state. */
    @Synchronized fun cancelJob(jobId: Int, timeoutMs: Int = 5_000): Pair<Boolean, String> {
        require(jobId > 0)
        return try {
            requireSuccess("cancel_job", command("cancel_job", listOf(jobId), timeoutMs))
            true to "Cancellation requested; awaiting device confirmation"
        } catch (e: Exception) {
            false to (e.message ?: "Cancellation outcome unknown")
        }
    }

    private fun canResumeSnapshot(status: ProPrinterStatus): Boolean = status.canResume &&
        (!status.raw.containsKey("job_id") || (status.jobId != null && status.jobId == currentJobId))

    /** Only the IO worker invokes this, after checking a fresh snapshot and current job. */
    private fun handleResume(status: ProPrinterStatus) {
        if (!status.hasFault) {
            resumedError = null
            resumeSentAt = null
            resumeRequested.set(false)
            return
        }
        if (resumeSentAt != null && status.errorCode != resumedError) {
            resumedError = null
            resumeSentAt = null
        }
        if (resumeSentAt != null && status.errorCode == resumedError && nowMs() - resumeSentAt!! > 15_000) {
            // Do not replay a side-effect command after a timeout. Require reconciliation.
            throw PrinterOutcomeUnknown("Recovery not confirmed; check the printer before retrying")
        }
        if (!resumeRequested.getAndSet(false) || cancelRequested.get()) return
        if (!canResumeSnapshot(status)) return
        if (resumeSentAt != null && status.errorCode == resumedError) return
        phase("resuming")
        resumedError = status.errorCode
        resumeSentAt = nowMs()
        try {
            requireSuccess("resume_printer", command("resume_printer", emptyMap<String, Any>(), 5_000))
        } catch (_: SocketTimeoutException) {
            // Poll the original job/snapshot; never blindly send resume a second time.
        }
    }

    private fun findJob(jobId: Int): Map<String, Any?>? {
        val response = jobInfo(jobId)
        requireSuccess("job_info", response)
        val result = response["result"] as? List<*> ?: throw PrinterError("job_info.result must be an array")
        @Suppress("UNCHECKED_CAST")
        return result.filterIsInstance<Map<String, Any?>>().find { strictInt(it["job_id"]) == jobId }
    }

    private fun terminal(job: Map<String, Any?>): Map<String, Any?>? = when (job["job_state"]) {
        "finished" -> { remoteJobSettled = true; settledJob = job; job }
        "canceled" -> { remoteJobSettled = true; throw PrinterCancelled() }
        "aborted" -> {
            remoteJobSettled = true
            val fault = runCatching { mixedStatus().errorCode }.getOrNull()
            throw PrinterError("Printer aborted job ${currentJobId ?: "unknown"}; device error ${fault ?: "unknown"}")
        }
        else -> null
    }

    /** Stop producing file batches, send once, and reconcile. A late finished wins over intent. */
    private fun handleCancel(): Boolean {
        if (!cancelRequested.get()) return false
        val jobId = currentJobId ?: throw PrinterCancelled()
        phase("canceling")
        if (!cancelSent) {
            cancelSent = true
            try {
                requireSuccess("cancel_job", command("cancel_job", listOf(jobId), 5_000))
            } catch (_: SocketTimeoutException) {
                // It may have been accepted. Read-only reconciliation is safe.
            }
        }
        val deadline = nowMs() + 15_000
        while (nowMs() < deadline) {
            val job = try { findJob(jobId) } catch (_: SocketTimeoutException) { null }
            if (job != null && terminal(job) != null) return true
            sleep(250)
        }
        throw PrinterOutcomeUnknown("Cancellation not confirmed for job $jobId; no automatic reprint")
    }

    @Synchronized
    fun printJpeg(
        jpeg: ByteArray,
        copies: Int = 1,
        onJobCreated: ((Int) -> Unit)? = null,
        onProgress: ((Int, Int) -> Unit)? = null,
    ): Int {
        require(jpeg.isNotEmpty() && copies in 1..9)
        require((jpeg.size.toLong() + CHUNK_BYTES - 1) / CHUNK_BYTES <= 65535)
        check(!jobCreationAttempted) { "Use a new session for each new print task" }
        if (cancelRequested.get()) throw PrinterCancelled()
        val sessionKey = key ?: throw PrinterError("Printer is not connected")
        jobCreationAttempted = true // A lost creation reply must NOT cause an automatic duplicate.
        val res = command("print_job", printJobParams(jpeg.size, copies, PHOTO_PRINT_JOB))
        val rejection = (res["error"] as? Map<*, *>)?.get("code") ?: res["error"]
        if (strictInt(rejection)?.let { it != 0 } == true) {
            // An explicit creation rejection has no accepted remote job to reconcile.
            remoteJobSettled = true
        }
        requireSuccess("print_job", res)
        val id = strictInt((res["result"] as? Map<*, *>)?.get("job_id"))?.takeIf { it > 0 }
            ?: throw PrinterOutcomeUnknown("print_job did not supply a valid job id")
        currentJobId = id
        onJobCreated?.invoke(id) // Persist before any image bytes, not after upload completion.
        if (!handleCancel()) sendFile(sessionKey, jpeg, id, onProgress)
        return id
    }

    @Synchronized
    fun waitUntilDone(
        jobId: Int,
        timeoutMs: Int = 180_000,
        pollMs: Long = 2_000,
        onState: ((String, Map<String, Any?>, Long) -> Unit)? = null,
    ): Map<String, Any?> {
        require(jobId > 0)
        if (currentJobId == null) currentJobId = jobId
        require(currentJobId == jobId)
        settledJob?.let { return it }
        val start = nowMs()
        var activeMs = 0L
        var lastTick = start
        var wasFaulted = false
        var lastState = "unknown"
        var missedPolls = 0
        while (nowMs() - start < timeoutMs + 900_000L) {
            if (handleCancel()) return settledJob!!
            val status: ProPrinterStatus
            try {
                val job = findJob(jobId)
                if (job != null) {
                    val state = job["job_state"] as? String ?: "unknown"
                    lastState = state
                    onState?.invoke(state, job, nowMs() - start)
                    terminal(job)?.let { phase(null); return it }
                }
                status = mixedStatus()
                missedPolls = 0
            } catch (e: SocketTimeoutException) {
                lastStatus = null
                onStatus(null, controlPhase)
                if (++missedPolls >= 3) throw PrinterOutcomeUnknown("Status polling timed out; printer outcome unknown")
                sleep(pollMs)
                continue
            }
            val now = nowMs()
            if (!wasFaulted) activeMs += now - lastTick
            lastTick = now
            wasFaulted = status.hasFault
            if (status.hasFault) {
                phase(if (resumeSentAt != null && status.errorCode == resumedError) "resuming" else "waiting_for_user")
            } else phase(null)
            handleResume(status)
            if (activeMs >= timeoutMs) break
            sleep(pollMs)
        }
        throw PrinterOutcomeUnknown("Job $jobId timed out (last state: $lastState); no automatic reprint")
    }

    private fun sendFile(sessionKey: ByteArray, bytes: ByteArray, jobId: Int, onProgress: ((Int, Int) -> Unit)?) {
        val total = (bytes.size + CHUNK_BYTES - 1) / CHUNK_BYTES
        val batch = java.io.ByteArrayOutputStream()
        var saved = 0
        var writes = 0
        val started = nowMs()
        for (j in 0 until total) {
            if (cancelRequested.get()) {
                batch.reset() // Unsent full frames are discarded; an in-progress write finishes first.
                handleCancel()
                return
            }
            val from = j * CHUNK_BYTES
            val to = minOf(from + CHUNK_BYTES, bytes.size)
            val body = ByteArray(4 + to - from)
            writeU32LE(body, 0, jobId)
            System.arraycopy(bytes, from, body, 4, to - from)
            val seq = nextSn()
            batch.write(buildFrame(CHANNEL_FILE_ENC, INTERACTIVE_REQUEST, ENCODING_HEX,
                seq, seq, ENCRYPT_ECB_OFFSET, encryptEcb(sessionKey, body), total, j + 1))
            saved++
            if (saved == framesPerWrite || j == total - 1) {
                if (cancelRequested.get()) { batch.reset(); handleCancel(); return }
                pipe.write(batch.toByteArray()) // IOException may mean partial delivery: never replay.
                batch.reset()
                saved = 0
                writes++
                onProgress?.invoke(j + 1, total)
                if (interWritePaceMs > 0 && j < total - 1) sleep(interWritePaceMs)
            }
        }
        lastSendStats = SendStats(bytes.size, total, writes, nowMs() - started)
        log("upload job $jobId: $lastSendStats")
    }

    companion object {
        const val LOG_TAG = "Darkroom.SPP"

        /** Frames coalesced into one socket write; matches the official app. */
        const val MAX_FRAMES_PER_WRITE = 3

        fun parseLooseJson(text: String): Map<String, Any?>? {
            return try {
                jsonToMap(JSONObject(text))
            } catch (_: Exception) {
                null
            }
        }

        private fun jsonToMap(obj: JSONObject): Map<String, Any?> {
            val out = LinkedHashMap<String, Any?>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = wrap(obj.get(k))
            }
            return out
        }

        private fun wrap(value: Any?): Any? = when (value) {
            JSONObject.NULL, null -> null
            is JSONObject -> jsonToMap(value)
            is JSONArray -> (0 until value.length()).map { wrap(value.get(it)) }
            else -> value
        }
    }
}
