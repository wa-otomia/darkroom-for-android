package io.github.wa_otomia.darkroom.data.printer

interface BytePipe {
    fun write(data: ByteArray)
    fun readExact(n: Int, timeoutMs: Int): ByteArray
    fun close()
}
