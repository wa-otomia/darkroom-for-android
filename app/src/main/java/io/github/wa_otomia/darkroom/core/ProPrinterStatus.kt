package io.github.wa_otomia.darkroom.core

/** Source: Mi Home com.hannto.printer 1.1.15 (69), modules 12332/12311/13166.
 * Raw device, RPC and event error namespaces must remain separate. */
data class ProPrinterStatus(val raw: Map<String, Any?>) {
    val category: String? get() = raw["category"] as? String
    val subCategory: String? get() = raw["sub_category"] as? String
    val errorCode: Int? get() = strictInt(raw["error"])
    val jobId: Int? get() = strictInt(raw["job_id"])?.takeIf { it > 0 }
    val battery: Int? get() = strictInt(raw["battery"])
    val batteryTemperature: Int? get() = strictInt(raw["battery-temp"])
    val sensor: Int? get() = strictInt(raw["sensor"])
    val usbConnected: Boolean? get() = sensor?.let { it and 8 != 0 }
    val coverClosed: Boolean? get() = sensor?.let { it and 4 != 0 }
    val charging: Boolean? get() = when (battery) {
        in 0..5 -> false
        in 16..21 -> true
        else -> null
    }
    val batteryBand: Int? get() = when (val b = battery) {
        in 0..5 -> b
        in 16..21 -> b!! - 16
        else -> null
    }
    // Candidate percentage reported by firmware, NOT a conversion from battery bars.
    // The plugin contains samples 94/96, not a calibrated/hardware-verified guarantee.
    val reportedBatteryPercent: Double? get() = (raw["battery-level"] as? Number)?.toDouble()
        ?.takeIf { it.isFinite() && it in 0.0..100.0 }
    val hasFault: Boolean get() = category == "error"
    val canResume: Boolean get() = hasFault && errorCode in RESUMABLE_PRO_ERRORS && coverClosed != false
    val readyForNewJob: Boolean get() = category == "idle" && !raw.containsKey("job_id")

    companion object {
        val RESUMABLE_PRO_ERRORS = setOf(-7103, -7110, -7111, -7112, -7114, -7201, -7208)
        fun fromResponse(response: Map<String, Any?>): ProPrinterStatus {
            require(!hasRpcError(response)) { "mixed_status returned an RPC error" }
            val obj = response["result"] as? Map<*, *> ?: error("mixed_status.result must be an object")
            require(obj.keys.all { it is String }) { "Invalid mixed_status object" }
            @Suppress("UNCHECKED_CAST")
            return ProPrinterStatus((obj as Map<String, Any?>).toMap())
        }
    }
}

internal fun strictInt(value: Any?): Int? {
    val n = (value as? Number)?.toDouble() ?: return null
    if (!n.isFinite() || n < Int.MIN_VALUE || n > Int.MAX_VALUE || n % 1.0 != 0.0) return null
    return n.toInt()
}

/** An explicit numeric zero is not a failure. An unknown error object is not success. */
fun hasRpcError(response: Map<String, Any?>): Boolean {
    val value = response["error"] ?: return false
    return when (value) {
        is Number -> value.toDouble() != 0.0
        is Map<*, *> -> strictInt(value["code"]) != 0
        else -> true
    }
}

/** Require both a reply frame and an exact request id; never consume an unsolicited event. */
fun isRpcReply(frame: Frame, message: Map<String, Any?>, id: Int): Boolean =
    frame.channelId == CHANNEL_DATA_ENC && frame.interactive == INTERACTIVE_RESPONSE &&
        message["method"] == null && strictInt(message["id"]) == id &&
        (message.containsKey("result") || message.containsKey("error"))
