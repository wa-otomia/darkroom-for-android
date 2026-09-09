package app.darkroom.android.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PhotoLineageTest {
    private val a = PhotoMeta("a", "a.jpg", "", "", 1, 1, 1)
    private val b = PhotoMeta("b", "b.jpg", "", "", 1, 1, 1, parentId = "a")
    private val c = PhotoMeta("c", "c.jpg", "", "", 1, 1, 1, parentId = "b")
    private val d = PhotoMeta("d", "d.jpg", "", "", 1, 1, 1, parentId = "a")
    private val other = PhotoMeta("z", "z.jpg", "", "", 1, 1, 1)

    @Test
    fun resolveRootWalksToAncestor() {
        val photos = listOf(a, b, c, d, other)
        assertEquals("a", resolveRootId(photos, "a"))
        assertEquals("a", resolveRootId(photos, "b"))
        assertEquals("a", resolveRootId(photos, "c"))
        assertEquals("a", resolveRootId(photos, "d"))
        assertEquals("z", resolveRootId(photos, "z"))
    }

    @Test
    fun resolveRootBreaksCycles() {
        val x = PhotoMeta("x", "x.jpg", "", "", 1, 1, 1, parentId = "y")
        val y = PhotoMeta("y", "y.jpg", "", "", 1, 1, 1, parentId = "x")
        assertEquals("x", resolveRootId(listOf(x, y), "x"))
    }

    @Test
    fun relatedPhotosGroupFamily() {
        val photos = listOf(a, b, c, d, other)
        assertEquals(listOf("a", "b", "c", "d"), listRelatedPhotos(photos, "c").map { it.id }.sorted())
        assertEquals(listOf("z"), listRelatedPhotos(photos, "z").map { it.id })
    }

    @Test
    fun combinePrompt() {
        assertEquals("film look", combineGeneratePrompt("film look", ""))
        assertEquals("brighter", combineGeneratePrompt("", "brighter"))
        assertEquals("film look\n\n补充要求：\nbrighter", combineGeneratePrompt("film look", "brighter"))
        try {
            combineGeneratePrompt("  ", "")
            throw AssertionError("expected throw")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("预设") || e.message!!.contains("提示词"))
        }
    }

    @Test
    fun editBeforePrintNeedsPresetOrPrompt() {
        assertTrue(shouldEditBeforePrint("preset-1", ""))
        assertTrue(shouldEditBeforePrint(null, "brighter"))
        assertTrue(shouldEditBeforePrint(AI_PRESET_NONE, "brighter"))
        assertTrue(shouldEditBeforePrint("preset-1", "brighter"))
        assertFalse(shouldEditBeforePrint(null, ""))
        assertFalse(shouldEditBeforePrint("", "   "))
        assertFalse(shouldEditBeforePrint(AI_PRESET_NONE, ""))
    }

    @Test
    fun generatedFilename() {
        assertEquals("IMG_1234-生成.jpg", generatedPhotoFilename("IMG_1234.JPG"))
        assertEquals("photo-生成.jpg", generatedPhotoFilename(""))
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }
}
