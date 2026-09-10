package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPresetsTest {
    @Test
    fun seedStartsWithBuiltins() {
        val seeded = seedAiPresets()
        assertEquals(AI_PRESET_NONE, seeded.lastSelectedId)
        assertTrue(seeded.presets.isNotEmpty())
        assertEquals("人像精修", seeded.presets[0].title)
        assertTrue(seeded.presets.any { it.id == "storybook" })
        assertTrue(seeded.presets.any { it.id == "anime-key" })
    }

    @Test
    fun seedIsWellFormed() {
        val seeded = seedAiPresets().presets
        assertEquals(seeded.size, seeded.map { it.id }.toSet().size)
        assertTrue(seeded.all { it.category in AI_PRESET_CATEGORIES && it.category != "custom" })
        assertTrue(seeded.all { it.title.length <= 80 && it.prompt.length <= 4000 })
        assertTrue(seeded.none { it.id in LEGACY_PRESET_IDS })
    }

    @Test
    fun normalizeDropsBrokenRows() {
        val store = normalizeAiPresetStore(
            AiPresetStore(
                lastSelectedId = "gone",
                presets = listOf(
                    AiPreset("film", "胶片", "warm film", "portrait"),
                    AiPreset("", "bad", "x", "custom"),
                    AiPreset("x", "  ", "x", "custom"),
                ),
            ),
        )
        assertEquals(listOf(AiPreset("film", "胶片", "warm film", "portrait")), store.presets)
        assertEquals(AI_PRESET_NONE, store.lastSelectedId)
    }

    @Test
    fun normalizeKeepsLiveSelection() {
        val store = normalizeAiPresetStore(
            AiPresetStore(
                lastSelectedId = "film",
                presets = listOf(AiPreset("film", "胶片", "warm film", "portrait")),
            ),
        )
        assertEquals("film", store.lastSelectedId)
    }

    @Test
    fun unknownCategoryBecomesCustom() {
        val store = normalizeAiPresetStore(
            AiPresetStore(presets = listOf(AiPreset("x", "自定义", "prompt", "weird"))),
        )
        assertEquals("custom", store.presets[0].category)
    }

    @Test
    fun seedIdsAreRecognised() {
        assertTrue(isSeedPresetId("retouch"))
        assertTrue(isSeedPresetId("anime-key"))
        assertTrue(!isSeedPresetId("film"))
        assertTrue(!isSeedPresetId("custom-user"))
    }

    @Test
    fun mergeAddsMissingBuiltins() {
        val stored = normalizeAiPresetStore(
            AiPresetStore(
                lastSelectedId = "retouch",
                presets = listOf(AiPreset("retouch", "精修", "edited by user", "portrait")),
            ),
        )
        val merged = mergeBuiltinPresets(stored)
        assertTrue(merged.presets.any { it.id == "anime-key" })
        assertEquals("retouch", merged.presets[0].id)
        assertEquals("edited by user", merged.presets[0].prompt)
        assertEquals("retouch", merged.lastSelectedId)
    }

    @Test
    fun mergeRetiresLegacyBuiltinsButKeepsUserPresets() {
        val stored = normalizeAiPresetStore(
            AiPresetStore(
                lastSelectedId = "anime",
                presets = listOf(
                    AiPreset("film", "胶片暖调", "old", "portrait"),
                    AiPreset("anime", "高质量二次元", "old", "creative"),
                    AiPreset("uuid-1", "我的预设", "mine", "custom"),
                ),
            ),
        )
        val merged = mergeBuiltinPresets(stored)
        assertTrue(merged.presets.none { it.id in LEGACY_PRESET_IDS })
        assertTrue(merged.presets.any { it.id == "uuid-1" })
        assertEquals(seedAiPresets().presets.size + 1, merged.presets.size)
        assertEquals(AI_PRESET_NONE, merged.lastSelectedId)
    }

    @Test
    fun mergeIsNoOpWhenUpToDate() {
        val stored = seedAiPresets()
        assertTrue(mergeBuiltinPresets(stored) === stored)
    }

    @Test
    fun validateRequiresTitleAndPrompt() {
        try {
            validateAiPresetFields("  ", "ok", null)
            throw AssertionError("expected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("标题"))
        }
        try {
            validateAiPresetFields("ok", "", null)
            throw AssertionError("expected")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("提示词"))
        }
        assertEquals(
            ValidatedPresetFields("棚拍", "clean", "creative"),
            validateAiPresetFields(" 棚拍 ", " clean ", "creative"),
        )
    }
}
