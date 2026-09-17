package io.github.wa_otomia.darkroom.core

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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

    @Test
    fun `accepted mime treats declared type as a hint`() {
        assertTrue(isAcceptedShareMime(null))
        assertTrue(isAcceptedShareMime(""))
        assertTrue(isAcceptedShareMime("image/jpeg"))
        assertTrue(isAcceptedShareMime("image/jpg"))
        assertTrue(isAcceptedShareMime("image/*"))
        assertTrue(isAcceptedShareMime("IMAGE/JPEG; charset=utf-8"))
        assertTrue(isAcceptedShareMime("*/*"))
        assertFalse(isAcceptedShareMime("text/plain"))
        assertFalse(isAcceptedShareMime("application/pdf"))
    }
}
