package io.github.wa_otomia.darkroom.data.printer

import io.github.wa_otomia.darkroom.core.*
import java.net.SocketTimeoutException

/** Length-driven incremental decoder; ciphertext may contain 7e64. */
internal class PrinterFrameReader(private val pipe: BytePipe) {
    private val pending = ArrayList<Byte>(1045)
    fun read(timeoutMs: Int): Frame {
        val deadline = System.nanoTime() + timeoutMs.toLong() * 1_000_000
        while (true) {
            while (pending.size >= 2 && (u(0) != FRAME_HEAD || u(1) != VERSION)) pending.removeAt(0)
            if (pending.size >= 20) {
                val length = 22 + ((u(18) or (u(19) shl 8)) and 1023)
                if (pending.size >= length) {
                    val raw = pending.take(length).toByteArray()
                    val frame = runCatching { parseFrame(raw) }.getOrNull()
                    if (frame != null) {
                        repeat(length) { pending.removeAt(0) }
                        return frame
                    }
                    pending.removeAt(0) // Reject bad checksum/tail; resync without dropping a next frame.
                    continue
                }
            }
            val remaining = ((deadline - System.nanoTime()) / 1_000_000).toInt()
            if (remaining <= 0) throw SocketTimeoutException("Printer frame timeout")
            pending.add(pipe.readExact(1, remaining)[0])
        }
    }
    private fun u(index: Int): Int = pending[index].toInt() and 255
}
