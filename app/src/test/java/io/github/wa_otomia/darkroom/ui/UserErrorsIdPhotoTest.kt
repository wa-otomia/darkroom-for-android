package io.github.wa_otomia.darkroom.ui

import io.github.wa_otomia.darkroom.R
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_ASPECT
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_BACKGROUND_KEPT
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_LOCAL_FAILED
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_MISSING
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NEED_KEY
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NOT_BINARY
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NO_BACKGROUND
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_NO_PERSON
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_SHADED
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_UNDECODABLE
import io.github.wa_otomia.darkroom.core.ID_MASK_MSG_VERSION_GONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserErrorsIdPhotoTest {
    @Test
    fun everyMaskMessageHasItsOwnString() {
        assertEquals(R.string.id_error_mask_aspect, userErrorRes(ID_MASK_MSG_ASPECT))
        assertEquals(R.string.id_error_mask_not_binary, userErrorRes(ID_MASK_MSG_NOT_BINARY))
        assertEquals(R.string.id_error_mask_no_person, userErrorRes(ID_MASK_MSG_NO_PERSON))
        assertEquals(R.string.id_error_mask_no_background, userErrorRes(ID_MASK_MSG_NO_BACKGROUND))
        assertEquals(R.string.id_error_mask_undecodable, userErrorRes(ID_MASK_MSG_UNDECODABLE))
        assertEquals(R.string.id_error_mask_version_gone, userErrorRes(ID_MASK_MSG_VERSION_GONE))
        assertEquals(R.string.id_error_mask_missing, userErrorRes(ID_MASK_MSG_MISSING))
    }

    @Test
    fun mattingFailuresHaveTheirOwnStrings() {
        assertEquals(R.string.id_error_mask_background_kept, userErrorRes(ID_MASK_MSG_BACKGROUND_KEPT))
        assertEquals(R.string.id_error_mask_shaded, userErrorRes(ID_MASK_MSG_SHADED))
        assertEquals(R.string.id_error_mask_local_failed, userErrorRes(ID_MASK_MSG_LOCAL_FAILED))
        // The local failure carries the exception class after the prefix.
        assertEquals(R.string.id_error_mask_local_failed, userErrorRes("$ID_MASK_MSG_LOCAL_FAILED：UnsatisfiedLinkError"))
        assertEquals(
            R.string.id_error_mask_local_failed,
            userErrorRes("java.lang.IllegalStateException: $ID_MASK_MSG_LOCAL_FAILED：OutOfMemoryError"),
        )
        assertNull(userErrorDetail(ID_MASK_MSG_SHADED))
    }

    @Test
    fun needKeyIsNotClaimedByTheGenericKeyRule() {
        // "未配置 AI" also matches error_grok_key; the mask rule must come first.
        assertEquals(R.string.id_error_mask_need_key, userErrorRes(ID_MASK_MSG_NEED_KEY))
    }

    @Test
    fun wrappedMessagesStillMap() {
        assertEquals(R.string.id_error_mask_missing, userErrorRes("java.lang.IllegalStateException: $ID_MASK_MSG_MISSING"))
        assertEquals(R.string.id_error_mask_aspect, userErrorRes("  $ID_MASK_MSG_ASPECT \n"))
    }

    @Test
    fun maskSaveFailuresMap() {
        assertEquals(R.string.id_error_mask_save, userErrorRes("无法保存证件照蒙版"))
        assertEquals(R.string.id_error_mask_save, userErrorRes("PNG 编码失败"))
    }

    @Test
    fun maskMessagesShowNoRawDetail() {
        assertNull(userErrorDetail(ID_MASK_MSG_MISSING))
        assertNull(userErrorDetail(ID_MASK_MSG_NOT_BINARY))
    }

    @Test
    fun existingRulesUnchanged() {
        assertEquals(R.string.print_cancelled, userErrorRes("已取消"))
        assertEquals(R.string.error_grok_key, userErrorRes("未配置 AI 密钥"))
        assertEquals(R.string.error_decode_jpeg, userErrorRes("无法解码 JPEG"))
    }
}
