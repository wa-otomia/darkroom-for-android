package app.darkroom.android.data.printer

import app.darkroom.android.core.CHANNEL_AUTH
import app.darkroom.android.core.CHANNEL_DATA_ENC
import app.darkroom.android.core.CHANNEL_FILE_ENC
import app.darkroom.android.core.CHUNK_BYTES
import app.darkroom.android.core.ENCODING_HEX
import app.darkroom.android.core.ENCODING_JSON
import app.darkroom.android.core.ENCRYPT_ECB_OFFSET
import app.darkroom.android.core.FRAME_HEAD
import app.darkroom.android.core.INTERACTIVE_CLIENT_CONFIRM
import app.darkroom.android.core.INTERACTIVE_REQUEST
import app.darkroom.android.core.INTERACTIVE_SERVER_CONFIRM
import app.darkroom.android.core.INTERACTIVE_SERVER_HELLO
import app.darkroom.android.core.NO_PAPER_MESSAGE
import app.darkroom.android.core.PHOTO_PRINT_JOB
import app.darkroom.android.core.VERSION
import app.darkroom.android.core.bodyLengthFromAttr
import app.darkroom.android.core.buildFrame
import app.darkroom.android.core.buildHello
import app.darkroom.android.core.decryptEcb
import app.darkroom.android.core.deriveSession
import app.darkroom.android.core.detectJobFailure
import app.darkroom.android.core.detectNoPaperJobs
import app.darkroom.android.core.encodeRpc
import app.darkroom.android.core.encryptEcb
import app.darkroom.android.core.formatPrintJobError
import app.darkroom.android.core.isActive
import app.darkroom.android.core.isNoPaperRpcError
import app.darkroom.android.core.isNoPaperState
import app.darkroom.android.core.isSuccessState
import app.darkroom.android.core.isTerminal
import app.darkroom.android.core.jobErrorMessage
import app.darkroom.android.core.jobIdMatches
import app.darkroom.android.core.noPaperFromRpc
import app.darkroom.android.core.normalizeJobList
import app.darkroom.android.core.parseFrame
import app.darkroom.android.core.printJobParams
import app.darkroom.android.core.readU16LE
import app.darkroom.android.core.writeU32LE
import org.json.JSONArray
import org.json.JSONObject

open class PrinterError(message: String) : Exception(message)
class HandshakeError(message: String) : PrinterError(message)
class NoPaperError(message: String = NO_PAPER_MESSAGE) : PrinterError(message) {
    val code: String = "NO_PAPER"
}

class XiaomiPrinter(
    private val pipe: BytePipe,
    private val readTimeout: Int = 8_000,
) {
    var key: ByteArray? = null
        private set
    private var sn = 0
    @Volatile
    private var pendingCancelJobId: Int? = null

    fun requestCancel(jobId: Int) {
        pendingCancelJobId = jobId
    }

    private fun throwIfCancelled() {
        if (pendingCancelJobId != null) error("已取消")
    }

    fun connect() {
        handshake()
    }

    fun disconnect() {
        try {
            pipe.close()
        } finally {
            key = null
        }
    }

    private fun nextSn(): Int {
        sn += 1
        return sn
    }

    private fun readFrame(): app.darkroom.android.core.Frame {
        val deadline = System.currentTimeMillis() + readTimeout
        while (System.currentTimeMillis() < deadline) {
            val b = pipe.readExact(1, (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1))
            if ((b[0].toInt() and 0xFF) != FRAME_HEAD) continue
            val nb = pipe.readExact(1, (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1))
            if ((nb[0].toInt() and 0xFF) != VERSION) continue
            val headerRest = pipe.readExact(18, (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1))
            val header = b + nb + headerRest
            val msgAttr = readU16LE(header, 18)
            val bodyLen = bodyLengthFromAttr(msgAttr)
            val rest = pipe.readExact(bodyLen + 2, (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1))
            return parseFrame(header + rest)
        }
        error("read timeout")
    }

    private fun readUntil(channelId: Int, interactive: Int, timeoutMs: Int = 10_000): app.darkroom.android.core.Frame {
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < timeoutMs) {
            val f = readFrame()
            if (f.channelId == channelId && f.interactive == interactive) return f
        }
        error("expected frame not received")
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

    fun command(method: String, params: Map<String, Any>, timeoutMs: Int = 15_000): Map<String, Any?> {
        val sessionKey = key ?: throw PrinterError("not connected (call connect())")
        val id = nextSn()
        val payload = encodeRpc(method, id, params)
        val enc = encryptEcb(sessionKey, payload)
        pipe.write(
            buildFrame(
                channelId = CHANNEL_DATA_ENC,
                interactive = INTERACTIVE_REQUEST,
                encoding = ENCODING_JSON,
                arcMsgSn = id,
                msgSn = id,
                encryptOffset = ENCRYPT_ECB_OFFSET,
                body = enc,
            ),
        )
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < timeoutMs) {
            val f = readFrame()
            if (f.channelId != CHANNEL_DATA_ENC) continue
            val dec = decryptEcb(sessionKey, f.body, stripZeros = true)
            val res = parseLooseJson(String(dec, Charsets.UTF_8)) ?: continue
            val rid = res["id"]
            if (rid == null || (rid is Number && rid.toInt() == id) || rid.toString() == id.toString()) return res
        }
        error("no response to $method")
    }

    fun jobInfo(jobId: Int, timeoutMs: Int = 20_000) = command("job_info", mapOf("job_id" to jobId), timeoutMs)

    fun checkPaperReady(timeoutMs: Int = 8_000) {
        val info = jobInfo(0, timeoutMs)
        val blocked = detectNoPaperJobs(info)
        if (blocked != null) throw NoPaperError(blocked)
    }

    fun cancelJob(jobId: Int, timeoutMs: Int = 5_000): Pair<Boolean, String> {
        return try {
            val res = command("cancel_job", mapOf("job_id" to jobId), timeoutMs)
            noPaperFromRpc(res)?.let { return false to it }
            if (res["error"] != null) {
                val err = res["error"]
                val code = (err as? Map<*, *>)?.get("code") as? Number
                false to if (code != null) "无法取消（错误码 ${code.toInt()}）" else "无法取消"
            } else {
                true to "已请求取消"
            }
        } catch (e: Exception) {
            val message = e.message ?: e.toString()
            if (message.contains("timeout", true) || message.contains("no response", true)) {
                false to "无法取消（打印机无响应）"
            } else {
                false to "无法取消（$message）"
            }
        }
    }

    fun printJpeg(jpeg: ByteArray, copies: Int = 1, onProgress: ((Int, Int) -> Unit)? = null): Int {
        val sessionKey = key ?: throw PrinterError("not connected")
        val res = command("print_job", printJobParams(jpeg.size, copies, PHOTO_PRINT_JOB))
        noPaperFromRpc(res)?.let { throw NoPaperError(it) }
        val result = res["result"] as? Map<*, *>
        val jobId = (result?.get("job_id") as? Number)?.toInt()
        if (jobId == null) {
            val msg = formatPrintJobError(res)
            if (isNoPaperRpcError(res) || msg.contains("缺纸")) throw NoPaperError(NO_PAPER_MESSAGE)
            throw PrinterError(msg)
        }
        sendFile(sessionKey, jpeg, jobId, onProgress)
        return jobId
    }

    fun waitUntilDone(
        jobId: Int,
        timeoutMs: Int = 180_000,
        pollMs: Long = 2000,
        onState: ((String, Map<String, Any?>, Long) -> Unit)? = null,
    ): Map<String, Any?> {
        val t0 = System.currentTimeMillis()
        var last: Map<String, Any?> = emptyMap()
        var polled = false
        while (System.currentTimeMillis() - t0 < timeoutMs) {
            val cancelId = pendingCancelJobId
            if (cancelId != null) {
                val (ok, msg) = cancelJob(cancelId)
                pendingCancelJobId = null
                if (!ok) throw PrinterError(msg)
                throw PrinterError("已取消")
            }
            if (!polled) {
                polled = true
                Thread.sleep(400)
            }
            val info = try {
                jobInfo(jobId, 25_000)
            } catch (e: Exception) {
                val message = e.message ?: ""
                if (message.contains("timeout", true) || message.contains("EIO", true)) {
                    Thread.sleep(pollMs)
                    continue
                }
                throw e
            }
            val jobs = normalizeJobList(info["result"])
            val job = jobs.find { jobIdMatches(it, jobId) }
            if (job != null) {
                last = job
                val state = job["job_state"]?.toString().orEmpty()
                if (state.isNotEmpty()) onState?.invoke(state, job, System.currentTimeMillis() - t0)
                val failure = detectJobFailure(job)
                if (failure != null) {
                    if (failure.contains("缺纸")) throw NoPaperError(failure)
                    if (job["job_state"]?.toString() == "aborted") rethrowIfNoPaperAfterAborted()
                    throw PrinterError(failure)
                }
                if (isTerminal(state)) {
                    if (isSuccessState(state)) return job
                    if (isNoPaperState(state)) throw NoPaperError(jobErrorMessage(state, job))
                    throw PrinterError(jobErrorMessage(state, job))
                }
            }
            Thread.sleep(pollMs)
        }
        val state = last["job_state"]?.toString().orEmpty()
        val failure = if (last["job_id"] != null) detectJobFailure(last) else null
        if (failure != null) {
            if (failure.contains("缺纸")) throw NoPaperError(failure)
            if (state == "aborted") rethrowIfNoPaperAfterAborted()
            throw PrinterError(failure)
        }
        if (isSuccessState(state)) return last
        if (isNoPaperState(state)) throw NoPaperError(jobErrorMessage(state, last))
        throw PrinterError("打印超时（${(System.currentTimeMillis() - t0) / 1000}s，最后状态：${state.ifEmpty { "unknown" }}）")
    }

    private fun rethrowIfNoPaperAfterAborted() {
        try {
            val gate = jobInfo(0, 6_000)
            val blocked = detectNoPaperJobs(gate)
            if (blocked != null) throw NoPaperError(blocked)
        } catch (e: NoPaperError) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun sendFile(sessionKey: ByteArray, fileBytes: ByteArray, jobId: Int, onProgress: ((Int, Int) -> Unit)?) {
        val total = (fileBytes.size + CHUNK_BYTES - 1) / CHUNK_BYTES
        val pending = ArrayList<ByteArray>()
        var saved = 0
        for (j in 0 until total) {
            throwIfCancelled()
            val from = j * CHUNK_BYTES
            val to = minOf(from + CHUNK_BYTES, fileBytes.size)
            val piece = fileBytes.copyOfRange(from, to)
            val body = ByteArray(4 + piece.size)
            writeU32LE(body, 0, jobId)
            System.arraycopy(piece, 0, body, 4, piece.size)
            val enc = encryptEcb(sessionKey, body)
            pending += buildFrame(
                channelId = CHANNEL_FILE_ENC,
                interactive = INTERACTIVE_REQUEST,
                encoding = ENCODING_HEX,
                arcMsgSn = nextSn(),
                msgSn = sn,
                encryptOffset = ENCRYPT_ECB_OFFSET,
                body = enc,
                pkgTotal = total,
                pkgNum = j + 1,
            )
            saved += 1
            if (saved >= 3 || j == total - 1) {
                val blob = pending.fold(ByteArray(0)) { acc, b -> acc + b }
                pending.clear()
                saved = 0
                pipe.write(blob)
                if (j < total - 1) Thread.sleep(65)
            }
            if (onProgress != null && (j == 0 || (j + 1) % 5 == 0)) onProgress(j + 1, total)
        }
        onProgress?.invoke(total, total)
    }

    companion object {
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
