package io.github.wa_otomia.darkroom.core

const val FRAME_HEAD: Int = 0x7E
const val FRAME_TAIL: Int = 0x7E
const val VERSION: Int = 0x64

const val CHANNEL_DATA = 1
const val CHANNEL_FILE = 2
const val CHANNEL_DATA_ENC = 3
const val CHANNEL_FILE_ENC = 4
const val CHANNEL_AUTH = 255

const val INTERACTIVE_REQUEST = 6
const val INTERACTIVE_RESPONSE = 7
const val INTERACTIVE_CLIENT_HELLO = 16
const val INTERACTIVE_SERVER_HELLO = 17
const val INTERACTIVE_CLIENT_CONFIRM = 18
const val INTERACTIVE_SERVER_CONFIRM = 19

const val ENCODING_BINARY = 1
const val ENCODING_HEX = 2
const val ENCODING_JSON = 3

const val ENCRYPT_NONE = 0
const val ENCRYPT_ECB_OFFSET = 5120
const val MULTI_PACKET = 8192

const val CHUNK_BYTES = 988
const val PHOTO_PRINT_JOB = 0
// JSON platform/source channel. Not the binary frame channel (3/4).
const val DEFAULT_FILE_CHANNEL = 64
const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
const val RFCOMM_CHANNEL = 1

// Pro job states: only a reported "finished" is success. Pending is NOT no-paper.
val ACTIVE_STATES = setOf(
    "pending", "downloading", "printing", "queued", "waiting", "printing_Y",
    "printing_M", "printing_C", "printing_OC", "home_feed", "cool_down",
)
val SUCCESS_STATES = setOf("finished")
val NO_PAPER_STATES = setOf("no_paper", "out_of_paper", "paper_empty", "paper_out", "lack_paper", "load_paper")
val ERROR_STATES = setOf("aborted", "canceled")
val TERMINAL_STATES = SUCCESS_STATES + ERROR_STATES

// Kept as a source-compatible constant; -6002 is a generic RPC system error.
@Deprecated("RPC -6002 does not mean no paper; use mixed_status.result.error")
const val RPC_NO_PAPER = -6002
const val NO_PAPER_MESSAGE = "Printer has no paper; load paper and resume the existing job"

data class Frame(
    val channelId: Int,
    val interactive: Int,
    val encoding: Int,
    val arcMsgSn: Int,
    val msgSn: Int,
    val pkgTotal: Int,
    val pkgNum: Int,
    val msgAttr: Int,
    val body: ByteArray,
)

fun toHex(bytes: ByteArray): String =
    bytes.joinToString(" ") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

fun checksum(frame: ByteArray): Int {
    var sum = 0
    for (b in frame) sum += b.toInt() and 0xFF
    return (sum - FRAME_HEAD) and 0xFF
}

fun verifyChecksum(frame: ByteArray): Boolean {
    if (frame.size < 22) return false
    val buf = frame.copyOf()
    val stored = buf[buf.size - 2].toInt() and 0xFF
    buf[buf.size - 2] = 0
    buf[buf.size - 1] = 0
    return stored == checksum(buf)
}

fun bodyLengthFromAttr(msgAttr: Int): Int {
    var attr = msgAttr
    if (attr >= MULTI_PACKET) attr -= MULTI_PACKET
    return attr % 1024
}

fun writeU32LE(buf: ByteArray, off: Int, n: Int) {
    buf[off] = (n and 0xFF).toByte()
    buf[off + 1] = ((n ushr 8) and 0xFF).toByte()
    buf[off + 2] = ((n ushr 16) and 0xFF).toByte()
    buf[off + 3] = ((n ushr 24) and 0xFF).toByte()
}

fun writeU16LE(buf: ByteArray, off: Int, n: Int) {
    buf[off] = (n and 0xFF).toByte()
    buf[off + 1] = ((n ushr 8) and 0xFF).toByte()
}

fun readU32LE(buf: ByteArray, off: Int): Int {
    return (buf[off].toInt() and 0xFF) or
        ((buf[off + 1].toInt() and 0xFF) shl 8) or
        ((buf[off + 2].toInt() and 0xFF) shl 16) or
        ((buf[off + 3].toInt() and 0xFF) shl 24)
}

fun readU16LE(buf: ByteArray, off: Int): Int {
    return (buf[off].toInt() and 0xFF) or ((buf[off + 1].toInt() and 0xFF) shl 8)
}

fun buildFrame(
    channelId: Int,
    interactive: Int,
    encoding: Int,
    arcMsgSn: Int,
    msgSn: Int,
    encryptOffset: Int,
    body: ByteArray,
    pkgTotal: Int = 0,
    pkgNum: Int = 0,
): ByteArray {
    val n = body.size
    require(n <= 1023) { "Frame body exceeds the 10-bit length field" }
    require(pkgTotal in 0..65535 && pkgNum in 0..65535) { "Invalid package numbering" }
    val frame = ByteArray(22 + n)
    frame[0] = FRAME_HEAD.toByte()
    frame[1] = VERSION.toByte()
    frame[2] = 0
    frame[3] = channelId.toByte()
    frame[4] = interactive.toByte()
    frame[5] = encoding.toByte()
    writeU32LE(frame, 6, arcMsgSn)
    writeU32LE(frame, 10, msgSn)
    var msgAttr = n + encryptOffset
    if (pkgTotal > 1) msgAttr += MULTI_PACKET
    writeU16LE(frame, 14, pkgTotal)
    writeU16LE(frame, 16, pkgNum)
    writeU16LE(frame, 18, msgAttr)
    System.arraycopy(body, 0, frame, 20, n)
    frame[frame.size - 2] = checksum(frame).toByte()
    frame[frame.size - 1] = FRAME_TAIL.toByte()
    return frame
}

fun parseFrame(frame: ByteArray): Frame {
    if (frame.size < 22) error("frame too short")
    if ((frame[0].toInt() and 0xFF) != FRAME_HEAD || (frame[frame.size - 1].toInt() and 0xFF) != FRAME_TAIL) {
        error("missing 0x7E delimiters")
    }
    val msgAttr = readU16LE(frame, 18)
    val n = bodyLengthFromAttr(msgAttr)
    require(frame[1].toInt() and 0xFF == VERSION) { "Unsupported frame version" }
    require(frame.size == 22 + n) { "Frame length mismatch" }
    require(verifyChecksum(frame)) { "Frame checksum mismatch" }
    return Frame(
        channelId = frame[3].toInt() and 0xFF,
        interactive = frame[4].toInt() and 0xFF,
        encoding = frame[5].toInt() and 0xFF,
        arcMsgSn = readU32LE(frame, 6),
        msgSn = readU32LE(frame, 10),
        pkgTotal = readU16LE(frame, 14),
        pkgNum = readU16LE(frame, 16),
        msgAttr = msgAttr,
        body = frame.copyOfRange(20, 20 + n),
    )
}

fun buildHello(msgSn: Int = 1): ByteArray = buildFrame(
    channelId = CHANNEL_AUTH,
    interactive = INTERACTIVE_CLIENT_HELLO,
    encoding = ENCODING_JSON,
    arcMsgSn = msgSn,
    msgSn = msgSn,
    encryptOffset = ENCRYPT_ECB_OFFSET,
    body = "hello".toByteArray(Charsets.UTF_8),
)

/** Params may be an object OR an array. Never stringify arrays as an object field. */
fun encodeRpc(method: String, id: Int, params: Any): ByteArray {
    require(params is Map<*, *> || params is List<*>) { "RPC params must be an object or array" }
    return jsonValue(linkedMapOf("method" to method, "id" to id, "params" to params))
        .toByteArray(Charsets.UTF_8)
}

private fun jsonValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> buildString {
        append('"')
        for (c in value) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 32) append("\\u" + c.code.toString(16).padStart(4, '0')) else append(c)
        }
        append('"')
    }
    is Boolean -> value.toString()
    is Number -> {
        require(value.toDouble().isFinite()) { "Non-finite JSON number" }
        value.toString()
    }
    is List<*> -> value.joinToString(",", "[", "]") { jsonValue(it) }
    is Map<*, *> -> value.entries.joinToString(",", "{", "}") {
        require(it.key is String) { "JSON object keys must be strings" }
        jsonValue(it.key) + ":" + jsonValue(it.value)
    }
    else -> error("Unsupported JSON value: ${value.javaClass.name}")
}

fun printJobParams(fileSize: Int, copies: Int = 1, jobType: Int = PHOTO_PRINT_JOB): Map<String, Any> = mapOf(
    "file_size" to fileSize,
    "copies" to copies,
    "job_type" to jobType,
    "channel" to DEFAULT_FILE_CHANNEL,
)

fun isActive(state: String?): Boolean = ACTIVE_STATES.contains(state ?: "")
fun isTerminal(state: String?): Boolean {
    val s = state ?: ""
    return s.isNotEmpty() && TERMINAL_STATES.contains(s)
}
fun isSuccessState(state: String?): Boolean {
    val s = state ?: ""
    return s.isNotEmpty() && SUCCESS_STATES.contains(s)
}
fun isErrorState(state: String?): Boolean {
    val s = state ?: ""
    return s.isNotEmpty() && ERROR_STATES.contains(s)
}
fun isNoPaperState(state: String?): Boolean {
    val s = state ?: ""
    return s.isNotEmpty() && NO_PAPER_STATES.contains(s)
}

fun rpcErrorCode(res: Map<String, Any?>): Int? {
    val err = res["error"]
    if (err is Number) return err.toInt()
    if (err is Map<*, *>) {
        val code = err["code"]
        if (code is Number) return code.toInt()
    }
    return null
}

fun isNoPaperRpcError(@Suppress("UNUSED_PARAMETER") res: Map<String, Any?>): Boolean = false

fun noPaperFromRpc(res: Map<String, Any?>): String? =
    if (isNoPaperRpcError(res)) NO_PAPER_MESSAGE else null

fun formatPrintJobError(res: Map<String, Any?>): String {
    val code = rpcErrorCode(res)
    if (code != null) return "无法创建打印任务（错误码 $code）"
    return "print_job failed: $res"
}

fun jobErrorMessage(state: String?, @Suppress("UNUSED_PARAMETER") job: Map<String, Any?>? = null): String =
    "Printer job state: ${state ?: "unknown"}; inspect mixed_status for device faults"

fun detectJobFailure(job: Map<String, Any?>): String? = when (job["job_state"]) {
    "aborted" -> "Printer aborted the job; query mixed_status for the device fault"
    "canceled" -> "Printer confirmed cancellation"
    else -> null
}

fun normalizeJobList(result: Any?): List<Map<String, Any?>> {
    return when (result) {
        is List<*> -> result.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            item as? Map<String, Any?>
        }
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            listOf(result as Map<String, Any?>)
        }
        else -> emptyList()
    }
}

fun detectNoPaperJobs(info: Map<String, Any?>): String? {
    noPaperFromRpc(info)?.let { return it }
    for (job in normalizeJobList(info["result"])) {
        val failure = detectJobFailure(job)
        if (failure?.contains("缺纸") == true) return failure
    }
    return null
}

fun jobIdMatches(record: Map<String, Any?>, jobId: Int): Boolean {
    val id = record["job_id"]
    return when (id) {
        is Number -> id.toInt() == jobId
        is String -> id.toIntOrNull() == jobId
        else -> false
    }
}

const val CAPTURED_HELLO_HEX = "7e 64 00 ff 10 03 01 00 00 00 01 00 00 00 00 00 00 00 05 14 68 65 6c 6c 6f a5 7e"
