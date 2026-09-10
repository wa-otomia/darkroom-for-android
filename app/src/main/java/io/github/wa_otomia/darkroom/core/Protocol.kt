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
const val DEFAULT_FILE_CHANNEL = 576
const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
const val RFCOMM_CHANNEL = 1

val ACTIVE_STATES = setOf("downloading", "printing", "queued", "waiting")
val SUCCESS_STATES = setOf("done", "success", "finished")
val NO_PAPER_STATES = setOf(
    "pending",
    "no_paper",
    "out_of_paper",
    "paper_empty",
    "paper_out",
    "lack_paper",
    "load_paper",
)
val ERROR_STATES = setOf(
    "error",
    "failed",
    "cancelled",
    "cancel",
    "aborted",
) + NO_PAPER_STATES
val TERMINAL_STATES = SUCCESS_STATES + ERROR_STATES

const val RPC_NO_PAPER = -6002
const val NO_PAPER_MESSAGE = "打印机缺纸，请装入相纸后重试"

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

fun encodeRpc(method: String, id: Int, params: Map<String, Any>): ByteArray {
    val paramJson = params.entries.joinToString(",") { (k, v) ->
        val value = when (v) {
            is String -> "\"$v\""
            is Number, is Boolean -> v.toString()
            else -> "\"$v\""
        }
        "\"$k\":$value"
    }
    return """{"method":"$method","id":$id,"params":{$paramJson}}""".toByteArray(Charsets.UTF_8)
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

private val PAPER_HINT = Regex("paper|缺纸|相纸|load\\s*paper|out\\s*of\\s*paper", RegexOption.IGNORE_CASE)

private val JOB_STATE_USER_MESSAGE = mapOf(
    "aborted" to "打印任务被中断（传输完成后打印机中止了任务：常见原因是缺纸、蓝牙断开或服务重启；请确认已装相纸且打印机空闲后再试）",
    "cancelled" to "打印已取消",
    "cancel" to "打印已取消",
    "failed" to "打印机报告任务失败，请检查相纸与电量后重试",
    "error" to "打印机报告错误，请检查相纸与电量后重试",
)

fun rpcErrorCode(res: Map<String, Any?>): Int? {
    val err = res["error"]
    if (err is Number) return err.toInt()
    if (err is Map<*, *>) {
        val code = err["code"]
        if (code is Number) return code.toInt()
    }
    return null
}

fun isNoPaperRpcError(res: Map<String, Any?>): Boolean = rpcErrorCode(res) == RPC_NO_PAPER

fun noPaperFromRpc(res: Map<String, Any?>): String? =
    if (isNoPaperRpcError(res)) NO_PAPER_MESSAGE else null

fun formatPrintJobError(res: Map<String, Any?>): String {
    val code = rpcErrorCode(res)
    if (code == RPC_NO_PAPER) return NO_PAPER_MESSAGE
    if (code != null) return "无法创建打印任务（错误码 $code）"
    return "print_job failed: $res"
}

fun jobErrorMessage(state: String?, job: Map<String, Any?>? = null): String {
    val s = state ?: ""
    if (isNoPaperState(s)) return NO_PAPER_MESSAGE
    if (job != null) {
        for (key in listOf("error", "err_msg", "error_msg", "message", "reason", "desc")) {
            val value = job[key]
            if (value is String && PAPER_HINT.containsMatchIn(value)) return NO_PAPER_MESSAGE
        }
    }
    JOB_STATE_USER_MESSAGE[s]?.let { return it }
    if (isErrorState(s)) return "打印失败：$s"
    return "打印失败：${s.ifEmpty { "unknown" }}"
}

fun detectJobFailure(job: Map<String, Any?>): String? {
    val state = job["job_state"]?.toString().orEmpty()
    val message = jobErrorMessage(state, job)
    if (isNoPaperState(state)) return message
    if (message.contains("缺纸")) return message
    if (isErrorState(state)) return message
    return null
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
