package io.github.wa_otomia.darkroom.core

import java.io.ByteArrayOutputStream
import java.io.InputStream

const val MAX_SHARED_IMAGES = 10
const val MAX_SHARED_IMAGE_BYTES = 25L * 1024L * 1024L

fun InputStream.readSharedImageBytes(maxBytes: Long = MAX_SHARED_IMAGE_BYTES): ByteArray {
    require(maxBytes in 1..Int.MAX_VALUE.toLong())
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        if (total > maxBytes) error("shared image exceeds ${maxBytes / (1024 * 1024)} MB limit")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
