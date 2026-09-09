package app.darkroom.android.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkTest {
    @Test
    fun requiredFromAndroid17() {
        assertFalse(needsLocalNetworkPermission(36))
        assertTrue(needsLocalNetworkPermission(37))
        assertTrue(needsLocalNetworkPermission(38))
    }

    @Test
    fun notRequiredOnOlderSdks() {
        assertFalse(needsLocalNetworkPermission(29))
        assertFalse(needsLocalNetworkPermission(33))
        assertFalse(needsLocalNetworkPermission(LOCAL_NETWORK_PERMISSION_SDK - 1))
    }
}
