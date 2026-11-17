package io.github.wa_otomia.darkroom.ui.studio

import io.github.wa_otomia.darkroom.core.ID_PHOTO_PRESETS
import io.github.wa_otomia.darkroom.core.IdBackground
import io.github.wa_otomia.darkroom.core.IdFeather
import io.github.wa_otomia.darkroom.core.idPhotoLayout
import io.github.wa_otomia.darkroom.core.idPhotoPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class IdPhotoLabelsTest {
    @Test
    fun millimetreLabels() {
        assertEquals(
            listOf(
                "35×45", "33×48", "25×35", "22×32", "35×49", "50.8×50.8",
                "50×70", "30×40", "24×30", "40×60", "35×50",
            ),
            ID_PHOTO_PRESETS.map { idPresetMm(it) },
        )
    }

    @Test
    fun sheetLabelNamesSizeAndSheet() {
        val preset = idPhotoPreset("25x35")
        assertEquals("25×35 mm · 1560×1040", idSheetLabel(preset, idPhotoLayout(preset)))
    }

    @Test
    fun everyPresetHasItsOwnPurpose() {
        val res = ID_PHOTO_PRESETS.map { idPresetPurposeRes(it.id) }
        assertEquals(ID_PHOTO_PRESETS.size, res.toSet().size)
    }

    @Test
    fun everyBackgroundHasNameAndUse() {
        val names = IdBackground.entries.map { idBackgroundNameRes(it) }
        val uses = IdBackground.entries.map { idBackgroundUseRes(it) }
        assertEquals(IdBackground.entries.size, names.toSet().size)
        assertEquals(IdBackground.entries.size, uses.toSet().size)
    }

    @Test
    fun everyFeatherHasALabel() {
        assertEquals(IdFeather.entries.size, IdFeather.entries.map { idFeatherLabelRes(it) }.toSet().size)
    }
}
