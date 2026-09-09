package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class ImportNameTest {
    private val now = Instant.parse("2026-09-09T14:53:00Z").toEpochMilli()
    private val stamp = "import-20260909-145300.jpg"

    @Test
    fun blankOrNumericFallsBackToTimestampedName() {
        val zone = ZoneOffset.UTC
        assertEquals(stamp, fallbackImportName(null, now, zone))
        assertEquals(stamp, fallbackImportName("", now, zone))
        assertEquals(stamp, fallbackImportName("   ", now, zone))
        assertEquals(stamp, fallbackImportName("265", now, zone))
        assertEquals(stamp, fallbackImportName("265.jpg", now, zone))
        assertEquals(stamp, fallbackImportName("image/265", now, zone))
    }

    @Test
    fun realDisplayNameIsKept() {
        assertEquals("dr_import_test.jpg", fallbackImportName("dr_import_test.jpg", now, ZoneOffset.UTC))
        assertEquals("photo.HEIC", fallbackImportName("DCIM/photo.HEIC", now, ZoneOffset.UTC))
        assertEquals("IMG_0001.png", fallbackImportName("IMG_0001.png", now, ZoneOffset.UTC))
    }
}
