package app.darkroom.android.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtpSecurityTest {
    @Test
    fun allowsExactlyWlan0AndWlan1() {
        assertTrue(isAllowedFtpInterface("wlan0"))
        assertTrue(isAllowedFtpInterface("wlan1"))
    }

    @Test
    fun rejectsNullBlankAndOtherIfaces() {
        assertFalse(isAllowedFtpInterface(null))
        assertFalse(isAllowedFtpInterface(""))
        assertFalse(isAllowedFtpInterface("WLAN0"))
        assertFalse(isAllowedFtpInterface("wlan0 "))
        assertFalse(isAllowedFtpInterface("wlan2"))
        assertFalse(isAllowedFtpInterface("lo"))
        assertFalse(isAllowedFtpInterface("ap0"))
        assertFalse(isAllowedFtpInterface("rmnet0"))
        assertFalse(isAllowedFtpInterface("unknown"))
    }
}
