package app.darkroom.android.core

import app.darkroom.android.data.catalog.toEntity
import app.darkroom.android.data.catalog.toMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoTagsTest {
    @Test
    fun tagsDefaultEmptyAndNotGenerated() {
        val photo = PhotoMeta("p", "a.jpg", "", "", 1, 1, 1)
        assertEquals(emptyList<String>(), photo.tags)
        assertFalse(photo.isGenerated())
    }

    @Test
    fun generatedTagMarksPhoto() {
        val photo = PhotoMeta("p", "a.jpg", "", "", 1, 1, 1, tags = listOf(GENERATED_PHOTO_TAG))
        assertTrue(photo.isGenerated())
    }

    @Test
    fun parentIdStillCountsAsGenerated() {
        val photo = PhotoMeta("p", "a.jpg", "", "", 1, 1, 1, parentId = "root")
        assertTrue(photo.isGenerated())
    }

    @Test
    fun unrelatedTagsAreNotGenerated() {
        val photo = PhotoMeta("p", "a.jpg", "", "", 1, 1, 1, tags = listOf("favorite"))
        assertFalse(photo.isGenerated())
    }

    @Test
    fun tagsSurviveEntityRoundTrip() {
        val photo = PhotoMeta(
            "p",
            "a.jpg",
            "c",
            "i",
            1,
            1,
            1,
            tags = listOf(GENERATED_PHOTO_TAG, "other"),
        )
        assertEquals(photo.tags, photo.toEntity().toMeta().tags)
    }
}
