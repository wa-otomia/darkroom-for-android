package io.github.wa_otomia.darkroom.core

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShareIngestTest {
    @Test
    fun `bounded read accepts input at limit`() {
        val bytes = ByteArray(32) { it.toByte() }

        assertArrayEquals(bytes, ByteArrayInputStream(bytes).readSharedImageBytes(32))
    }

    @Test
    fun `bounded read rejects input over limit`() {
        assertThrows(IllegalStateException::class.java) {
            ByteArrayInputStream(ByteArray(33)).readSharedImageBytes(32)
        }
    }
}
