package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoEditPresetTest {
    private val library = listOf(
        AiPreset("retouch", "人像精修", "clean skin", "portrait"),
        AiPreset("mono", "经典黑白", "black and white", "portrait"),
        AiPreset("mine", "我的胶片", "warm film", "custom"),
    )

    @Test
    fun resolveTreatsMissingAndEmptyAsNone() {
        assertEquals(AI_PRESET_NONE, resolveAutoEditPresetId(null, library))
        assertEquals(AI_PRESET_NONE, resolveAutoEditPresetId("", library))
        assertEquals(AI_PRESET_NONE, resolveAutoEditPresetId("   ", library))
    }

    @Test
    fun resolveKeepsExistingIdTrimmed() {
        assertEquals("mono", resolveAutoEditPresetId("mono", library))
        assertEquals("mono", resolveAutoEditPresetId("  mono ", library))
    }

    @Test
    fun resolveDropsUnknownIdWithoutFallback() {
        assertEquals(AI_PRESET_NONE, resolveAutoEditPresetId("gone", library))
    }

    @Test
    fun neverSavedMigratesFromLastSelected() {
        assertEquals("retouch", reconcileAutoEditPresetId(null, "retouch", library))
    }

    @Test
    fun neverSavedWithoutUsableDefaultGivesNone() {
        assertEquals(AI_PRESET_NONE, reconcileAutoEditPresetId(null, AI_PRESET_NONE, library))
        assertEquals(AI_PRESET_NONE, reconcileAutoEditPresetId(null, "gone", library))
    }

    @Test
    fun savedNoneIsNotOverwrittenByLastSelected() {
        assertEquals(AI_PRESET_NONE, reconcileAutoEditPresetId("", "retouch", library))
    }

    @Test
    fun savedIdWinsOverLastSelected() {
        assertEquals("mono", reconcileAutoEditPresetId("mono", "retouch", library))
    }

    @Test
    fun reconcileIsIdempotent() {
        val inputs = listOf<String?>(null, "", "mono", "gone", " mine ")
        for (stored in inputs) {
            for (last in listOf(AI_PRESET_NONE, "retouch", "gone")) {
                val once = reconcileAutoEditPresetId(stored, last, library)
                assertEquals(once, reconcileAutoEditPresetId(once, last, library))
            }
        }
    }

    @Test
    fun retiredBuiltinIdIsClearedNotMapped() {
        val merged = mergeBuiltinPresets(
            AiPresetStore(
                presets = listOf(AiPreset("anime", "动漫", "old anime", "creative")),
                lastSelectedId = "anime",
            ),
        )
        assertEquals(AI_PRESET_NONE, reconcileAutoEditPresetId("anime", merged.lastSelectedId, merged.presets))
        assertEquals(AI_PRESET_NONE, reconcileAutoEditPresetId(null, merged.lastSelectedId, merged.presets))
    }

    @Test
    fun deletingTheAutomationPresetClearsIt() {
        assertEquals(AI_PRESET_NONE, autoEditPresetAfterDelete("mine", "mine"))
    }

    @Test
    fun deletingAnotherPresetKeepsIt() {
        assertEquals("mono", autoEditPresetAfterDelete("mono", "mine"))
        assertEquals(AI_PRESET_NONE, autoEditPresetAfterDelete(AI_PRESET_NONE, "mine"))
    }
}
