package io.github.wa_otomia.darkroom.core

import io.github.wa_otomia.darkroom.data.catalog.toEntity
import io.github.wa_otomia.darkroom.data.catalog.toMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FramingTest {
    private val portrait = Framing(landscape = false, zoom = 1.4f, offsetX = 12f, offsetY = -8f)
    private val landscapeEdit = Framing(landscape = true, zoom = 0.8f, rotationDegrees = 90f)

    @Test
    fun framingVersionKeyNormalizesBlankToOriginal() {
        assertEquals(ORIGINAL_VERSION_ID, framingVersionKey(null))
        assertEquals(ORIGINAL_VERSION_ID, framingVersionKey(""))
        assertEquals(ORIGINAL_VERSION_ID, framingVersionKey("original"))
        assertEquals("e1", framingVersionKey("e1"))
    }

    @Test
    fun framingsAreIndependentPerVersion() {
        val photo = PhotoMeta("p", "a.jpg", "", "", 2000, 3000, 1)
            .withFraming("original", portrait)
            .withFraming("e1", landscapeEdit)
        assertEquals(portrait, photo.framingFor("original"))
        assertEquals(landscapeEdit, photo.framingFor("e1"))
        assertNull(photo.framingFor("e2"))
        val cleared = photo.withFraming("e1", null)
        assertEquals(portrait, cleared.framingFor("original"))
        assertNull(cleared.framingFor("e1"))
        assertEquals(setOf(ORIGINAL_VERSION_ID), cleared.framings.keys)
    }

    @Test
    fun galleryTileFramingUsesNewestVersionSlot() {
        val older = EditRecord("e1", "", "2026-01-01T00:00:00Z", "e1.jpg")
        val newer = EditRecord("e2", "", "2026-03-01T00:00:00Z", "e2.jpg")
        val photo = PhotoMeta(
            "p",
            "a.jpg",
            "",
            "",
            2000,
            3000,
            1,
            edits = listOf(older, newer),
            framings = mapOf(
                ORIGINAL_VERSION_ID to portrait,
                "e1" to landscapeEdit,
                "e2" to Framing(landscape = false, zoom = 2f),
            ),
        )
        assertEquals(2f, galleryTileFraming(photo)?.zoom)
        assertEquals(portrait, galleryTileFraming(photo) { false })
        assertNull(galleryTileFraming(photo.copy(framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit))))
    }

    @Test
    fun placementRoundTripKeepsPose() {
        val placement = ViewportPlacement(
            zoom = 1.5f,
            offsetX = 20f,
            offsetY = -10f,
            rotationDegrees = 15f,
            frameWidth = 400f,
            frameHeight = 600f,
            imageWidth = 3000,
            imageHeight = 2000,
        )
        val framing = placement.toFraming(landscape = true)
        val back = framing.toPlacement()
        assertEquals(placement.zoom, back.zoom)
        assertEquals(placement.offsetX, back.offsetX)
        assertEquals(placement.offsetY, back.offsetY)
        assertEquals(placement.rotationDegrees, back.rotationDegrees)
        assertEquals(placement.frameWidth, back.frameWidth)
        assertEquals(placement.frameHeight, back.frameHeight)
        assertTrue(framing.landscape)
    }

    @Test
    fun panInFrameScalesOffsetsToTheNewViewport() {
        val framing = Framing(landscape = false, offsetX = 20f, offsetY = -10f, frameWidth = 400f, frameHeight = 600f)
        val (x, y) = framing.panInFrame(200f, 300f)
        assertEquals(10f, x, 0.001f)
        assertEquals(-5f, y, 0.001f)
        assertEquals(20f to -10f, framing.panInFrame(0f, 300f))
    }

    @Test
    fun toPlacementFillsPrintSheetWhenFrameSizeMissing() {
        val framing = Framing(landscape = true, zoom = 1.2f, offsetX = 5f)
        val placement = framing.toPlacement()
        assertEquals(PRINT_HEIGHT.toFloat(), placement.frameWidth)
        assertEquals(PRINT_WIDTH.toFloat(), placement.frameHeight)
    }

    @Test
    fun matchesAutoDefaultAndPersistable() {
        assertTrue(Framing(landscape = false).matchesAutoDefault(2000, 3000))
        assertTrue(Framing(landscape = true).matchesAutoDefault(4000, 3000))
        assertFalse(Framing(landscape = true).matchesAutoDefault(2000, 3000))
        assertFalse(Framing(landscape = false, zoom = 1.2f).matchesAutoDefault(2000, 3000))
        assertNull(persistableFraming(Framing(landscape = false), 2000, 3000))
        assertEquals(1.2f, persistableFraming(Framing(landscape = false, zoom = 1.2f), 2000, 3000)?.zoom)
    }

    @Test
    fun resolveQueuedPoseUsesSavedOnlyWhenCallerOmitsPose() {
        val saved = Framing(landscape = true, zoom = 1.3f, rotationDegrees = 90f, frameWidth = 100f, frameHeight = 150f)
        val fromSaved = resolveQueuedPose(saved, null, 0f, null)
        assertEquals(true, fromSaved.landscape)
        assertEquals(90f, fromSaved.rotationDegrees)
        assertEquals(1.3f, fromSaved.placement?.zoom)

        val studio = resolveQueuedPose(saved, false, 0f, ViewportPlacement(1f, 0f, 0f, 0f, 10f, 20f, 1, 1))
        assertEquals(false, studio.landscape)
        assertEquals(1f, studio.placement?.zoom)

        val none = resolveQueuedPose(null, null, 0f, null)
        assertNull(none.landscape)
        assertNull(none.placement)
    }

    @Test
    fun resolveExecutionPoseUsesSavedFramingForManualAndAutomation() {
        val saved = Framing(
            landscape = true,
            zoom = 1.5f,
            offsetX = 12f,
            offsetY = -4f,
            rotationDegrees = 12f,
            frameWidth = 800f,
            frameHeight = 500f,
            imageWidth = 3000,
            imageHeight = 2000,
        )
        val manual = resolveExecutionPose(saved, manual = true, autoLandscape = false)
        val automated = resolveExecutionPose(saved, manual = false, autoLandscape = false)
        assertEquals(true, manual.landscape)
        assertEquals(12f, manual.rotationDegrees, 0f)
        assertEquals(saved.toPlacement(), manual.placement)
        assertEquals(manual, automated)
    }

    @Test
    fun resolveExecutionPoseManualWithoutSavedUsesSheetDefaultNotFit() {
        val portrait = resolveExecutionPose(null, manual = true, autoLandscape = false)
        assertEquals(false, portrait.landscape)
        assertEquals(0f, portrait.rotationDegrees, 0f)
        val portraitPlacement = portrait.placement
        assertNotNull(portraitPlacement)
        assertEquals(1f, portraitPlacement!!.zoom, 0f)
        assertEquals(0f, portraitPlacement.offsetX, 0f)
        assertEquals(0f, portraitPlacement.offsetY, 0f)
        assertEquals(0f, portraitPlacement.rotationDegrees, 0f)
        assertEquals(PRINT_WIDTH.toFloat(), portraitPlacement.frameWidth, 0f)
        assertEquals(PRINT_HEIGHT.toFloat(), portraitPlacement.frameHeight, 0f)

        val landscape = resolveExecutionPose(null, manual = true, autoLandscape = true)
        assertEquals(true, landscape.landscape)
        val landscapePlacement = landscape.placement
        assertNotNull(landscapePlacement)
        assertEquals(1f, landscapePlacement!!.zoom, 0f)
        assertEquals(0f, landscapePlacement.offsetX, 0f)
        assertEquals(0f, landscapePlacement.offsetY, 0f)
        assertEquals(0f, landscapePlacement.rotationDegrees, 0f)
        assertEquals(PRINT_HEIGHT.toFloat(), landscapePlacement.frameWidth, 0f)
        assertEquals(PRINT_WIDTH.toFloat(), landscapePlacement.frameHeight, 0f)
    }

    @Test
    fun resolveExecutionPoseAutomationWithoutSavedLeavesPlacementUnset() {
        val portrait = resolveExecutionPose(null, manual = false, autoLandscape = false)
        assertEquals(false, portrait.landscape)
        assertEquals(0f, portrait.rotationDegrees, 0f)
        assertNull(portrait.placement)

        val landscape = resolveExecutionPose(null, manual = false, autoLandscape = true)
        assertEquals(true, landscape.landscape)
        assertEquals(0f, landscape.rotationDegrees, 0f)
        assertNull(landscape.placement)
    }

    @Test
    fun alignedEditFramingFillsWithinOnePercentTolerance() {
        assertEquals(0.01, AI_ASPECT_TOLERANCE, 0.0)
        val matching = alignedEditFraming(1024, 1536, landscape = false)
        val rounding = alignedEditFraming(784, 1168, landscape = false)
        val justInside = alignedEditFraming(1034, 1536, landscape = false)
        val justOutside = alignedEditFraming(1036, 1536, landscape = false)
        assertEquals(1f, matching.zoom, 0f)
        assertEquals(1f, rounding.zoom, 0f)
        assertEquals(1f, justInside.zoom, 0f)
        assertTrue(justOutside.zoom < 1f)
        assertEquals(0.988f, justOutside.zoom, 0.001f)
        assertEquals(0f, justOutside.offsetX, 0f)
        assertEquals(0f, justOutside.offsetY, 0f)
        assertEquals(0f, justOutside.rotationDegrees, 0f)
    }

    @Test
    fun alignedEditFramingSquareIsAboutTwoThirdsAndPersists() {
        val square = alignedEditFraming(1024, 1024, landscape = false)
        assertEquals(0.667f, square.zoom, 0.001f)
        assertEquals(false, square.landscape)
        assertEquals(0f, square.offsetX, 0f)
        assertEquals(0f, square.offsetY, 0f)
        assertEquals(PRINT_WIDTH.toFloat(), square.frameWidth, 0f)
        assertEquals(PRINT_HEIGHT.toFloat(), square.frameHeight, 0f)
        assertEquals(1024, square.imageWidth)
        assertEquals(1024, square.imageHeight)
        val stored = persistableFraming(square, 1024, 1024)
        assertEquals(false, stored?.landscape)
        assertEquals(0.667f, stored?.zoom ?: 0f, 0.001f)
    }

    @Test
    fun alignedEditFramingWideAspectUsesMinimumZoomAndStaysCentered() {
        val wide = alignedEditFraming(1920, 1080, landscape = false)
        assertEquals(MIN_PRINT_ZOOM, wide.zoom, 0f)
        assertTrue(wide.zoom > 0.375f)
        assertEquals(0f, wide.offsetX, 0f)
        assertEquals(0f, wide.offsetY, 0f)
        assertEquals(0f, wide.rotationDegrees, 0f)
        assertNotNull(persistableFraming(wide, 1920, 1080))
        assertEquals(1f, alignedEditFraming(0, 1080, landscape = false).zoom, 0f)
        assertEquals(1f, alignedEditFraming(1920, -1, landscape = true).zoom, 0f)
    }

    @Test
    fun alignedEditFramingBannerAndDefaultPoseAreNotStored() {
        val portrait = alignedEditFraming(1024, 1536, landscape = false)
        val nearPortrait = alignedEditFraming(784, 1168, landscape = false)
        val banner = alignedEditFraming(1536, 1024, landscape = true)
        assertEquals(1f, portrait.zoom, 0f)
        assertEquals(1f, nearPortrait.zoom, 0f)
        assertEquals(1f, banner.zoom, 0f)
        assertEquals(true, banner.landscape)
        assertEquals(PRINT_HEIGHT.toFloat(), banner.frameWidth, 0f)
        assertEquals(PRINT_WIDTH.toFloat(), banner.frameHeight, 0f)
        assertEquals(1536, banner.imageWidth)
        assertEquals(1024, banner.imageHeight)
        assertNull(persistableFraming(portrait, 1024, 1536))
        assertNull(persistableFraming(nearPortrait, 784, 1168))
        assertNull(persistableFraming(banner, 1536, 1024))
    }

    @Test
    fun framingsSurviveEntityRoundTrip() {
        val photo = PhotoMeta(
            "p",
            "a.jpg",
            "c",
            "i",
            1,
            1,
            1,
            framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit),
        )
        assertEquals(photo.framings, photo.toEntity().toMeta().framings)
    }
}
