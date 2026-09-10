package io.github.wa_otomia.darkroom.core

import kotlinx.serialization.Serializable
import java.util.UUID

const val AI_PRESET_NONE = ""

val AI_PRESET_CATEGORIES = listOf("portrait", "creative", "custom")

@Serializable
data class AiPreset(
    val id: String,
    val title: String,
    val prompt: String,
    val category: String,
)

@Serializable
data class AiPresetStore(
    val presets: List<AiPreset>,
    val lastSelectedId: String = AI_PRESET_NONE,
)

data class EditPresetSeed(
    val id: String,
    val label: String,
    val prompt: String,
    val category: String,
)

/**
 * Built-in presets, in display order.
 *
 * "portrait" = practical fixes that must leave the photo recognisably untouched apart from the one
 * thing they promise. "creative" = deliberate restyles; the people still have to stay recognisable.
 * Every prompt forbids cropping because the print pipeline crops separately.
 */
val EDIT_PRESETS = listOf(
    // ---- 实用修图（不乱动，只做标题说的那一件事） ----
    EditPresetSeed(
        "retouch",
        "人像精修",
        "Professional portrait retouch with strict fidelity. Only clean the skin: remove temporary blemishes (acne, pimples, red patches), reduce oily shine, slightly soften dark under-eye circles, remove stray flyaway hairs and lint on clothing. Keep natural skin texture and pores; keep moles, freckles, scars and wrinkles that define this person. Do NOT reshape the face, jaw, nose, eyes or body; do NOT whiten skin or teeth; do NOT add makeup; do NOT change expression, hairstyle, clothing, background, lighting, colors or framing. The result must look like the exact same photo taken on a good-skin day. No cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "auto-fix",
        "一键修正",
        "Technical correction only. Fix exposure (recover clipped highlights and crushed shadows), neutralise white balance so skin looks natural, reduce noise and slight softness. Do not restyle, do not shift colors beyond correction, do not alter people, expressions, clothing, background or framing. It should look like the same shot taken with better camera settings. No cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "print-ready",
        "打印增强",
        "Prepare this photo for a small 2x3 inch glossy print. Gently lift midtones, add a little contrast and saturation so it does not print flat, moderately sharpen fine detail, and protect skin tones from turning orange. Keep everything else identical: same people, framing and background, no added or removed objects, no cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "night-boost",
        "夜景提亮",
        "Brighten this dark, low-light photo naturally. Recover shadow detail in faces, reduce color noise and grain, but keep the night atmosphere (do not turn night into day) and keep real light sources with their warm colors. Do not change people, clothing, background content or framing. No halos, no washed-out flat look, no cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "declutter",
        "背景去杂",
        "Remove distracting background clutter only: trash, cables, signs, stray objects and unrelated passers-by that are clearly behind the subjects. Fill the removed areas with a plausible continuation of the surroundings. Keep every main subject fully intact: same faces, poses, clothing and anything they hold. Do not change lighting, colors, style or framing. No cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "glasses-glare",
        "眼镜去反光",
        "Remove reflections and glare on eyeglass lenses so the eyes behind them are clearly visible, reconstructing the eyes consistently with this person's real eye shape and gaze. Keep the frames, face, expression, hair, clothing, background, colors and framing exactly the same. Change nothing else. No cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "id-photo",
        "证件照白底",
        "Turn this into a clean ID/passport-style photo. Replace the entire background with a uniform pure white backdrop lit by soft, even frontal studio light with no shadows on the backdrop. Keep the person's face, hair, glasses, expression and clothing exactly as they are: no retouching of features, no makeup, no reshaping. Keep the same framing and aspect ratio, no cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "soft-light",
        "柔光人像",
        "Relight the portrait with soft, flattering beauty-dish style light: gentle catchlights in the eyes, smooth shadow transitions, no harsh shadows under the eyes or nose. Keep identity, facial features, expression, hairstyle, clothing, background and framing unchanged. Photorealistic, no glow filter, no skin smoothing beyond what the lighting alone would do. No cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "film-warm",
        "暖调胶片",
        "Color grade only: a warm analog film look like Kodak Portra. Soft warm skin tones, slightly lifted blacks, gentle highlight roll-off, fine subtle grain. This is purely a tonal and color change: keep the same people, faces, clothing, background, sharpness and framing. No added objects, no cropping.",
        "portrait",
    ),
    EditPresetSeed(
        "mono",
        "经典黑白",
        "Convert to classic black and white with a rich tonal range: deep but not crushed blacks, clean highlights, smooth midtone gradation and flattering skin luminance. Preserve all facial detail and texture. Purely a monochrome conversion: do not alter content, composition or framing. No cropping.",
        "portrait",
    ),

    // ---- 风格化（允许改风格，但人必须还认得出） ----
    EditPresetSeed(
        "hk-cinema",
        "港风电影",
        "Regrade in a 1990s Hong Kong art-film look: moody green-cyan shadows, warm tungsten and neon highlights, dense saturated color, soft halation around lights, light film grain. Keep the same people, poses, clothing, setting and framing recognisable; only the color, light and atmosphere change. Photorealistic, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "teal-orange",
        "电影青橙",
        "Cinematic color grade: teal shadows, warm orange skin tones, richer contrast, a subtle vignette and the wide tonal feel of a modern blockbuster frame. Keep the same people, clothing, background and framing; change only color and light. Photorealistic, no added objects, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "golden-hour",
        "黄金时刻",
        "Relight the scene as if shot at golden hour: low warm sunlight from one side, long soft shadows, glowing rim light on hair, warm sky tones. Keep the same people, expressions, clothing and location recognisable; only lighting, color and atmosphere change. Photorealistic, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "anime-key",
        "日系动画",
        "Redraw as a high-quality Japanese anime key visual: clean confident linework, cel shading with soft gradients, detailed expressive eyes and hair, painterly background of the same location. The people must stay recognisable: same face shape, hairstyle, expression, pose, clothing and composition. Not chibi, not sketchy, no low-resolution look, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "toon-3d",
        "3D 动画角色",
        "Re-render the people as charming 3D animated-film characters in a Pixar/Disney style: stylised yet clearly recognisable likeness, large expressive eyes, soft skin shading, detailed hair, cinematic lighting. Keep the same pose, expression, clothing, background setting and composition. Family-friendly, polished, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "watercolor",
        "水彩插画",
        "Repaint as a delicate watercolor illustration: soft wet-on-wet washes, gentle color bleeding, a light pencil underdrawing, plenty of white paper showing through in the highlights. Keep the same people, poses, clothing and composition recognisable. Airy and fresh, not muddy, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "oil-classic",
        "古典油画",
        "Repaint as a classical oil portrait in the manner of an old master: visible brushwork, rich layered pigments, warm chiaroscuro lighting, subtle canvas texture. Keep the same people, poses, clothing and composition clearly recognisable. Museum quality, no modern digital artifacts, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "pencil-sketch",
        "铅笔素描",
        "Convert to a realistic graphite pencil drawing on textured paper: confident outlines, careful cross-hatching and smooth shading, with the strongest detail on the faces. Keep the same people, poses and composition recognisable. Monochrome, no color, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "pop-comic",
        "美漫波普",
        "Restyle as a bold American comic-book / pop-art panel: thick ink outlines, flat saturated colors, halftone-dot shading, dramatic highlights. Keep the same people, poses, clothing and composition recognisable. No speech bubbles or text, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "neon-city",
        "赛博霓虹",
        "Reimagine the scene at night in a cyberpunk city: magenta and cyan neon signs, rain-slick reflective ground, holographic accents, cinematic haze. Keep the same people fully recognisable with the same faces, poses and clothing (subtle neon rim light is fine). Photorealistic sci-fi, no cropping.",
        "creative",
    ),
    EditPresetSeed(
        "storybook",
        "童话绘本",
        "Reimagine as a warm storybook fairy-tale illustration: soft pastel light, gentle bokeh, subtle magical sparkles and an enchanted whimsical setting. Keep the same people with their recognisable features, pose and composition. Warm and wholesome, no horror, no cropping.",
        "creative",
    ),
)

/**
 * Ids of built-ins from earlier releases that no longer exist. They are retired on the next read
 * so upgraded installs do not show two generations of presets side by side.
 */
val LEGACY_PRESET_IDS = setOf(
    "film", "studio", "cinema", "bw", "fix",
    "fairy-tale", "apocalypse", "anime", "cyberpunk", "oil-painting",
)

const val AUTO_PRINT_AI_PROMPT =
    "Keep the same people and composition. Improve exposure, white balance and color for a 2x3 inch photo print. Do not crop, do not change identity, clothing or background, do not add objects."

fun normalizeCategory(raw: String?): String =
    if (raw != null && raw in AI_PRESET_CATEGORIES) raw else "custom"

fun seedAiPresets(): AiPresetStore = AiPresetStore(
    presets = EDIT_PRESETS.map { AiPreset(it.id, it.label, it.prompt, it.category) },
    lastSelectedId = AI_PRESET_NONE,
)

fun normalizeAiPresetStore(raw: AiPresetStore?): AiPresetStore {
    val presets = raw?.presets.orEmpty()
        .map {
            AiPreset(
                id = it.id.trim(),
                title = it.title.trim(),
                prompt = it.prompt.trim(),
                category = normalizeCategory(it.category),
            )
        }
        .filter { it.id.isNotEmpty() && it.title.isNotEmpty() && it.prompt.isNotEmpty() }
    val last = raw?.lastSelectedId?.trim().orEmpty()
    return AiPresetStore(
        presets = presets,
        lastSelectedId = if (last.isNotEmpty() && presets.any { it.id == last }) last else AI_PRESET_NONE,
    )
}

fun mergeBuiltinPresets(stored: AiPresetStore): AiPresetStore {
    val seed = seedAiPresets()
    val kept = stored.presets.filter { it.id !in LEGACY_PRESET_IDS }
    var changed = kept.size != stored.presets.size
    val byId = kept.associateBy { it.id }.toMutableMap()
    for (builtin in seed.presets) {
        if (!byId.containsKey(builtin.id)) {
            byId[builtin.id] = builtin
            changed = true
        }
    }
    if (!changed) return stored
    val builtinIds = seed.presets.map { it.id }.toSet()
    val presets = seed.presets.map { byId[it.id]!! } + kept.filter { it.id !in builtinIds }
    val last = stored.lastSelectedId
    return stored.copy(
        presets = presets,
        lastSelectedId = if (presets.any { it.id == last }) last else AI_PRESET_NONE,
    )
}

data class ValidatedPresetFields(val title: String, val prompt: String, val category: String)

fun validateAiPresetFields(title: String?, prompt: String?, category: String?): ValidatedPresetFields {
    val t = title?.trim().orEmpty()
    val p = prompt?.trim().orEmpty()
    if (t.isEmpty()) throw IllegalArgumentException("请填写预设标题")
    if (p.isEmpty()) throw IllegalArgumentException("请填写预设提示词")
    if (t.length > 80) throw IllegalArgumentException("标题最多 80 字")
    if (p.length > 4000) throw IllegalArgumentException("提示词最多 4000 字")
    return ValidatedPresetFields(t, p, normalizeCategory(category))
}

fun newPresetId(): String = UUID.randomUUID().toString()

fun isSeedPresetId(id: String): Boolean = EDIT_PRESETS.any { it.id == id }
