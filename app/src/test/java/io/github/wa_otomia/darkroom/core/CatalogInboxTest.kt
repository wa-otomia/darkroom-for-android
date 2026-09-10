package io.github.wa_otomia.darkroom.core

import io.github.wa_otomia.darkroom.data.catalog.CatalogRepository
import io.github.wa_otomia.darkroom.data.catalog.selectStaleInboxFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CatalogInboxTest {
    @Test
    fun staleInboxSelectsOnlyOldFiles() {
        val dir = Files.createTempDirectory("inbox").toFile()
        val now = 20L * 60L * 1000L
        val maxAge = CatalogRepository.INBOX_MAX_AGE_MS
        val old = File(dir, "bad.jpg").apply {
            writeText("x")
            setLastModified(now - maxAge - 1)
        }
        val edge = File(dir, "a8-bad.jpg").apply {
            writeText("y")
            setLastModified(now - maxAge)
        }
        val fresh = File(dir, "ok.jpg").apply {
            writeText("z")
            setLastModified(now - 1_000L)
        }
        File(dir, "nested").mkdirs()
        val stale = selectStaleInboxFiles(dir.listFiles(), now, maxAge)
        assertEquals(setOf(old, edge), stale.toSet())
        assertTrue(fresh !in stale)
    }

    @Test
    fun staleInboxEmptyWhenMissing() {
        assertTrue(selectStaleInboxFiles(null, 0L, CatalogRepository.INBOX_MAX_AGE_MS).isEmpty())
        assertTrue(selectStaleInboxFiles(emptyArray(), 0L, CatalogRepository.INBOX_MAX_AGE_MS).isEmpty())
    }
}
