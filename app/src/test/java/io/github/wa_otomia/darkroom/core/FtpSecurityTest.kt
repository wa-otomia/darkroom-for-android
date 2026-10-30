package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtpSecurityTest {
    @Test
    fun allowsWifiConnectionInterfaces() {
        assertTrue(isAllowedFtpInterface("wlan0"))
        assertTrue(isAllowedFtpInterface("wlan1"))
        assertTrue(isAllowedFtpInterface("wlan2"))
        assertTrue(isAllowedFtpInterface("WLAN0"))
    }

    @Test
    fun allowsPhoneHotspotNamesAcrossVendors() {
        assertTrue(isAllowedFtpInterface("swlan0")) // Samsung
        assertTrue(isAllowedFtpInterface("ap0")) // MediaTek phones: Xiaomi, OPPO, vivo, Motorola...
        assertTrue(isAllowedFtpInterface("softap0")) // some Qualcomm builds: Xiaomi, Honor, Lenovo
        assertTrue(isAllowedFtpInterface("ap_br_wlan1")) // dual-band bridged hotspot, AOSP
        assertTrue(isAllowedFtpInterface("ap_br_ap0")) // dual-band bridged hotspot, MediaTek
        assertTrue(isAllowedFtpInterface("ap_br_softap0"))
    }

    @Test
    fun allowsAWifiDirectGroupThePhoneOwns() {
        assertTrue(isAllowedFtpInterface("p2p0"))
        assertTrue(isAllowedFtpInterface("p2p-wlan0-0"))
        assertTrue(isAllowedFtpInterface("p2p-p2p0-12"))
    }

    @Test
    fun allowsAnUnlistedHotspotNameWhileTheSystemReportsItAsTethered() {
        assertFalse(isAllowedFtpInterface("wl0.1"))
        assertTrue(isAllowedFtpInterface("wl0.1", FtpInterfacePolicy(tethered = setOf("wl0.1"))))
    }

    @Test
    fun rejectsMobileDataWhateverTheVendor() {
        for (name in listOf(
            "rmnet0", "rmnet1", "rmnet_data0", "rmnet_ipa0", "rmnet_mhi0", "rmnet_ims00",
            "ccmni0", "cc2mni0", "ccemni1", "ccinet0", "seth_lte0", "sipa_eth0", "eth_x0",
            "v4-rmnet_data0", "v4-ccmni0", "clat4",
        )) {
            assertFalse(name, isAllowedFtpInterface(name))
        }
    }

    @Test
    fun rejectsVpnUsbBluetoothEthernetAndLoopbackEvenIfReportedAsTethered() {
        val tetheredEverything = FtpInterfacePolicy(
            tethered = setOf("rndis0", "ncm0", "usb0", "bt-pan", "bnep0", "eth0", "tun0", "ppp0", "rmnet0"),
        )
        for (name in tetheredEverything.tethered + listOf("ipsec0", "dummy0", "lo")) {
            assertFalse(name, isAllowedFtpInterface(name, tetheredEverything))
        }
    }

    @Test
    fun anInterfaceTheSystemMarksAsMobileDataOrVpnIsRefusedEvenWithAWifiName() {
        val policy = FtpInterfacePolicy(tethered = setOf("ap0"), denied = setOf("wlan1", "ap0"))
        assertFalse(isAllowedFtpInterface("wlan1", policy))
        assertFalse(isAllowedFtpInterface("AP0", policy))
        assertTrue(isAllowedFtpInterface("wlan0", policy))
    }

    @Test
    fun rejectsMalformedNames() {
        assertFalse(isAllowedFtpInterface(null))
        assertFalse(isAllowedFtpInterface(""))
        assertFalse(isAllowedFtpInterface("wlan"))
        assertFalse(isAllowedFtpInterface("wlan0 "))
        assertFalse(isAllowedFtpInterface("ap"))
        assertFalse(isAllowedFtpInterface("unknown"))
    }
}
