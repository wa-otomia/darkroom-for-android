package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionLabelsTest {
    @Test
    fun versionLabelUsesPresetName() {
        assertEquals("1-人像精修", versionLabel(1, "人像精修"))
        assertEquals("2-暖调胶片", versionLabel(2, " 暖调胶片 "))
    }

    @Test
    fun versionLabelFallsBackWhenNameMissing() {
        assertEquals("1-修图", versionLabel(1, null))
        assertEquals("3-修图", versionLabel(3, "  "))
        assertEquals("1-Edit", versionLabel(1, null, "Edit"))
    }

    @Test
    fun resolvePhotoSourceTreatsBlankAsOriginal() {
        assertEquals("original", resolvePhotoSource(null))
        assertEquals("original", resolvePhotoSource(""))
        assertEquals("original", resolvePhotoSource("  "))
        assertEquals("edit-9", resolvePhotoSource("edit-9"))
    }

    @Test
    fun newestVersionSourcePicksLatestCreated() {
        val older = EditRecord("a", "", "2026-01-01T00:00:00Z", "a.jpg")
        val newer = EditRecord("b", "", "2026-02-01T00:00:00Z", "b.jpg")
        assertEquals("original", newestVersionSource(emptyList()))
        assertEquals("b", newestVersionSource(listOf(older, newer)))
        assertEquals("b", defaultStudioSource(PhotoMeta("p", "x.jpg", "", "", 1, 1, 1, edits = listOf(older, newer))))
        assertEquals("original", defaultStudioSource(null))
    }

    @Test
    fun sourceAfterVersionRemovedSwitchesOnlyWhenCurrentDeleted() {
        val first = EditRecord("a", "", "2026-01-01T00:00:00Z", "a.jpg")
        val second = EditRecord("b", "", "2026-02-01T00:00:00Z", "b.jpg")
        val edits = listOf(first, second)
        assertEquals("a", sourceAfterVersionRemoved(edits, "b", "a"))
        assertEquals("a", sourceAfterVersionRemoved(edits, "b", "b"))
        assertEquals("original", sourceAfterVersionRemoved(listOf(first), "a", "a"))
    }

    @Test
    fun resolveEditPresetNamePrefersStoredTitleThenId() {
        val titles = mapOf("retouch" to "人像精修")
        assertEquals("暖调胶片", resolveEditPresetName("暖调胶片", "retouch", "修图") { titles[it] })
        assertEquals("人像精修", resolveEditPresetName("", "retouch", "修图") { titles[it] })
        assertEquals("修图", resolveEditPresetName("", "missing", "修图") { titles[it] })
        assertEquals("修图", resolveEditPresetName(null, null, "修图"))
    }

    @Test
    fun studioVersionIngestNameUsesStemAndIndex() {
        assertEquals("DSC_0001-v2.jpg", studioVersionIngestName("DSC_0001.JPG", 2))
        assertEquals("photo-v1.jpg", studioVersionIngestName(".jpg", 1))
        assertEquals("shot-v3.jpg", studioVersionIngestName("folder/shot.png", 3))
    }
}
