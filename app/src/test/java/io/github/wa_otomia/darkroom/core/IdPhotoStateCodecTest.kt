package io.github.wa_otomia.darkroom.core

import io.github.wa_otomia.darkroom.data.catalog.toEntity
import io.github.wa_otomia.darkroom.data.catalog.toMeta
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IdPhotoStateCodecTest {
    private val legacyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val portrait = Framing(landscape = false, zoom = 1.4f, offsetX = 12f, offsetY = -8f)
    private val landscapeEdit = Framing(landscape = true, zoom = 0.8f, rotationDegrees = 90f)
    private val idOriginal = IdPhotoState(
        active = true,
        presetId = "33x48",
        zoom = 1.6f,
        panX = 0.12f,
        panY = -0.05f,
        rotation = 90f,
        background = "white",
        feather = "medium",
        maskRev = "a1b2c3d4e5f6",
    )
    private val idEdit = IdPhotoState(presetId = "25x35")

    private fun photo(
        framings: Map<String, Framing> = emptyMap(),
        idPhotos: Map<String, IdPhotoState> = emptyMap(),
    ) = PhotoMeta("p", "a.jpg", "c", "i", 1, 1, 1, framings = framings, idPhotos = idPhotos)

    @Test
    fun defaultsAndPose() {
        val s = IdPhotoState()
        assertFalse(s.active)
        assertEquals("35x45", s.presetId)
        assertEquals("original", s.background)
        assertEquals("weak", s.feather)
        assertNull(s.maskRev)
        assertFalse(s.hasMask)
        assertEquals(IdPhotoPose(), s.pose)
        val moved = s.withPose(IdPhotoPose(1.3f, 0.2f, -0.1f, 270f))
        assertEquals(IdPhotoPose(1.3f, 0.2f, -0.1f, 270f), moved.pose)
        assertEquals(s.presetId, moved.presetId)
        assertTrue(idOriginal.hasMask)
        assertFalse(IdPhotoState(maskRev = " ").hasMask)
    }

    @Test
    fun normalizedFallsBackForUnknownOrBrokenValues() {
        val broken = IdPhotoState(
            active = true,
            presetId = "99x99",
            zoom = Float.NaN,
            panX = Float.POSITIVE_INFINITY,
            panY = Float.NaN,
            rotation = -90f,
            background = "purple",
            feather = "fuzzy",
            maskRev = "  ",
        ).normalized()
        assertEquals(
            IdPhotoState(
                active = true,
                presetId = "35x45",
                zoom = 1f,
                panX = 0f,
                panY = 0f,
                rotation = 270f,
                background = "original",
                feather = "weak",
                maskRev = null,
            ),
            broken,
        )
        assertEquals(MAX_PRINT_ZOOM, IdPhotoState(zoom = 10f).normalized().zoom, 0f)
        assertEquals(MIN_PRINT_ZOOM, IdPhotoState(zoom = 0.1f).normalized().zoom, 0f)
        assertEquals(0f, IdPhotoState(rotation = Float.NaN).normalized().rotation, 0f)
        assertEquals(idOriginal, idOriginal.normalized())
    }

    @Test
    fun presetLookupFallsBackToDefault() {
        assertEquals("35x45", idPhotoPreset(null).id)
        assertEquals("35x45", idPhotoPreset("").id)
        assertEquals("35x45", idPhotoPreset("bogus").id)
        assertEquals("24x30", idPhotoPreset("24x30").id)
        assertEquals("35x45", normalizeIdPhotoPresetId("bogus"))
        assertEquals("50x70", normalizeIdPhotoPresetId("50x70"))
    }

    @Test
    fun perVersionAccessUsesFramingKeys() {
        val p = photo().withIdPhoto(null, idOriginal).withIdPhoto("e1", idEdit)
        assertEquals(idOriginal, p.idPhotoFor("original"))
        assertEquals(idOriginal, p.idPhotoFor(""))
        assertEquals(idEdit, p.idPhotoFor("e1"))
        assertNull(p.idPhotoFor("e2"))
        val removed = p.withIdPhoto("e1", null)
        assertNull(removed.idPhotoFor("e1"))
        assertEquals(idOriginal, removed.idPhotoFor(null))
        assertSame(removed, removed.withIdPhoto("e9", null))
        assertTrue(removed.framings.isEmpty())
    }

    @Test
    fun printAndMaskGates() {
        assertTrue(idPrintApplies(idOriginal, usedPreset = false))
        assertFalse(idPrintApplies(idOriginal, usedPreset = true))
        assertFalse(idPrintApplies(idOriginal.copy(active = false), usedPreset = false))
        assertFalse(idPrintApplies(null, usedPreset = false))
        assertTrue(idMaskRequired(idOriginal))
        assertFalse(idMaskRequired(idOriginal.copy(background = "original")))
        assertFalse(idMaskRequired(idOriginal.copy(background = "bogus")))
        assertFalse(idMaskRequired(null))
    }

    @Test
    fun roundTripKeepsFramingsAndIdStates() {
        val framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit)
        val ids = mapOf(ORIGINAL_VERSION_ID to idOriginal, "e2" to idEdit)
        val raw = encodeVersionStates(framings, ids)
        val (f, i) = decodeVersionStates(raw)
        assertEquals(framings, f)
        assertEquals(ids, i)
        assertTrue(raw.contains("\"idPhoto\""))
    }

    @Test
    fun withoutIdStateTheJsonIsTheLegacyFramingsJson() {
        val framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit)
        assertEquals(legacyJson.encodeToString(framings), encodeVersionStates(framings, emptyMap()))
        assertEquals("{}", encodeVersionStates(emptyMap(), emptyMap()))
    }

    @Test
    fun legacyJsonWithoutIdPhotoDecodes() {
        val framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit)
        val (f, i) = decodeVersionStates(legacyJson.encodeToString(framings))
        assertEquals(framings, f)
        assertTrue(i.isEmpty())
        val (f2, i2) = decodeVersionStates("{}")
        assertTrue(f2.isEmpty() && i2.isEmpty())
    }

    @Test
    fun corruptIdStateDropsOnlyThatVersionsIdState() {
        val raw = """
            {"original":{"landscape":false,"zoom":1.4,"offsetX":12.0,"offsetY":-8.0,"idPhoto":{"active":true,"zoom":"abc"}},
             "e1":{"landscape":true,"zoom":0.8,"rotationDegrees":90.0,"idPhoto":{"active":true,"presetId":"25x35"}},
             "e2":{"landscape":true,"idPhoto":7}}
        """.trimIndent()
        val (f, i) = decodeVersionStates(raw)
        assertEquals(portrait, f[ORIGINAL_VERSION_ID])
        assertEquals(landscapeEdit, f["e1"])
        assertEquals(Framing(landscape = true), f["e2"])
        assertNull(i[ORIGINAL_VERSION_ID])
        assertNull(i["e2"])
        assertEquals(IdPhotoState(active = true, presetId = "25x35"), i["e1"])
    }

    @Test
    fun idOnlyEntryWithoutLandscapeDecodes() {
        val raw = """{"e1":{"idPhoto":{"active":true,"presetId":"40x60","background":"navy","maskRev":"r1"}}}"""
        val (f, i) = decodeVersionStates(raw)
        assertTrue(f.isEmpty())
        assertEquals(
            IdPhotoState(active = true, presetId = "40x60", background = "navy", maskRev = "r1"),
            i["e1"],
        )
        // And it encodes back to an ID-only entry.
        val again = encodeVersionStates(emptyMap(), i)
        assertFalse(again.contains("landscape"))
        assertEquals(f to i, decodeVersionStates(again))
    }

    @Test
    fun decodedStatesAreNormalized() {
        val raw = """{"original":{"idPhoto":{"active":true,"presetId":"nope","zoom":9.0,"rotation":-90.0,"feather":"x"}}}"""
        val (_, i) = decodeVersionStates(raw)
        assertEquals(
            IdPhotoState(active = true, zoom = MAX_PRINT_ZOOM, rotation = 270f),
            i[ORIGINAL_VERSION_ID],
        )
    }

    @Test
    fun brokenFramingEntryDoesNotTakeOthersWithIt() {
        val raw = """
            {"original":{"landscape":"sideways","idPhoto":{"active":true}},
             "e1":{"landscape":true,"zoom":0.8,"rotationDegrees":90.0}}
        """.trimIndent()
        val (f, i) = decodeVersionStates(raw)
        assertNull(f[ORIGINAL_VERSION_ID])
        assertEquals(landscapeEdit, f["e1"])
        assertEquals(IdPhotoState(active = true), i[ORIGINAL_VERSION_ID])
    }

    @Test
    fun garbageGivesEmptyMaps() {
        for (raw in listOf("", "{", "not json", "[]", "null", "42", "\"x\"")) {
            val (f, i) = decodeVersionStates(raw)
            assertTrue(raw, f.isEmpty())
            assertTrue(raw, i.isEmpty())
        }
        // Non-object entries are skipped, not fatal.
        val (f, i) = decodeVersionStates("""{"original":5,"e1":{"landscape":true}}""")
        assertEquals(mapOf("e1" to Framing(landscape = true)), f)
        assertTrue(i.isEmpty())
    }

    @Test
    fun entityRoundTripCarriesIdStates() {
        val p = photo(
            framings = mapOf(ORIGINAL_VERSION_ID to portrait, "e1" to landscapeEdit),
            idPhotos = mapOf(ORIGINAL_VERSION_ID to idOriginal, "e2" to idEdit),
        )
        val back = p.toEntity().toMeta()
        assertEquals(p.framings, back.framings)
        assertEquals(p.idPhotos, back.idPhotos)
        assertEquals(p, back)
    }

    @Test
    fun entityWithoutIdStatesStoresLegacyJson() {
        val p = photo(framings = mapOf(ORIGINAL_VERSION_ID to portrait))
        val entity = p.toEntity()
        assertEquals(legacyJson.encodeToString(p.framings), entity.framingsJson)
        assertEquals(p, entity.toMeta())
    }

    @Test
    fun encodingNormalizesStates() {
        val raw = encodeVersionStates(emptyMap(), mapOf("e1" to IdPhotoState(zoom = Float.NaN, background = "bogus")))
        val (_, i) = decodeVersionStates(raw)
        assertEquals(IdPhotoState(), i["e1"])
    }
}
