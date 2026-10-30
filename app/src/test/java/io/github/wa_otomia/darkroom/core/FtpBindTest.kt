package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test

class FtpBindTest {
    @Test
    fun listensOnlyOnWlanInterfacesNeverOnMobileData() {
        val got = selectFtpBindTargets(
            listOf(
                FtpBindTarget("rmnet_data0", "10.64.12.3"),
                FtpBindTarget("rmnet1", "100.1.2.3"),
                FtpBindTarget("ccmni0", "10.20.30.40"),
                FtpBindTarget("seth_lte0", "10.9.9.9"),
                FtpBindTarget("v4-rmnet_data0", "192.0.0.4"),
                FtpBindTarget("wlan1", "10.82.121.117"),
                FtpBindTarget("wlan0", "192.168.0.20"),
                FtpBindTarget("lo", "127.0.0.1"),
            ),
        )
        assertEquals(
            listOf(
                FtpBindTarget("wlan0", "192.168.0.20"),
                FtpBindTarget("wlan1", "10.82.121.117"),
            ),
            got,
        )
    }

    @Test
    fun noWifiMeansNothingToBindNeverTheWildcard() {
        assertEquals(emptyList<FtpBindTarget>(), selectFtpBindTargets(emptyList()))
        assertEquals(
            emptyList<FtpBindTarget>(),
            selectFtpBindTargets(listOf(FtpBindTarget("rmnet_data0", "10.64.12.3"))),
        )
        assertEquals(
            emptyList<FtpBindTarget>(),
            selectFtpBindTargets(listOf(FtpBindTarget("*", "0.0.0.0"), FtpBindTarget("wlan0", "0.0.0.0"))),
        )
    }

    @Test
    fun phoneHotspotAndWifiDirectAreServedButTetheringOverUsbBluetoothAndVpnAreNot() {
        val got = selectFtpBindTargets(
            listOf(
                FtpBindTarget("ap0", "192.168.43.1"),
                FtpBindTarget("swlan0", "192.168.44.1"),
                FtpBindTarget("softap0", "192.168.45.1"),
                FtpBindTarget("p2p-wlan0-0", "192.168.49.1"),
                FtpBindTarget("rndis0", "192.168.42.129"),
                FtpBindTarget("bt-pan", "192.168.44.200"),
                FtpBindTarget("tun0", "10.8.0.2"),
                FtpBindTarget("wlan2", "192.168.50.1"),
            ),
        )
        assertEquals(
            listOf("ap0", "p2p-wlan0-0", "softap0", "swlan0", "wlan2"),
            got.map { it.iface },
        )
    }

    @Test
    fun runtimePolicyAddsAnUnlistedHotspotAndRemovesADeniedInterface() {
        val got = selectFtpBindTargets(
            listOf(
                FtpBindTarget("wl0.1", "192.168.43.1"),
                FtpBindTarget("wlan0", "192.168.0.20"),
                FtpBindTarget("wlan1", "10.0.0.5"),
            ),
            FtpInterfacePolicy(tethered = setOf("wl0.1"), denied = setOf("wlan1")),
        )
        assertEquals(listOf("wl0.1", "wlan0"), got.map { it.iface })
    }

    @Test
    fun duplicateAddressesBindOnce() {
        val got = selectFtpBindTargets(
            listOf(FtpBindTarget("wlan0", "192.168.0.20"), FtpBindTarget("WLAN0", "192.168.0.20")),
        )
        assertEquals(1, got.size)
    }

    @Test
    fun listenAddressesExcludeWildcardAndLoopbackUnlessATestAsks() {
        assertTrue(isFtpListenAddress("192.168.0.20"))
        assertFalse(isFtpListenAddress("0.0.0.0"))
        assertFalse(isFtpListenAddress("0.0.0.0", allowLoopback = true))
        assertFalse(isFtpListenAddress("*"))
        assertFalse(isFtpListenAddress("::"))
        assertFalse(isFtpListenAddress("255.255.255.255"))
        assertFalse(isFtpListenAddress("0.0.0.00"))
        assertFalse(isFtpListenAddress("000.000.000.000"))
        assertFalse(isFtpListenAddress("192.168.0.256"))
        assertFalse(isFtpListenAddress("127.000.000.001"))
        assertFalse(isFtpListenAddress("127.0.0.1"))
        assertTrue(isFtpListenAddress("127.0.0.1", allowLoopback = true))
    }

    @Test
    fun rebindPlanFollowsWifiChanges() {
        val home = FtpBindTarget("wlan0", "192.168.0.20")
        val office = FtpBindTarget("wlan0", "10.1.2.3")
        val hotspot = FtpBindTarget("wlan1", "192.168.43.1")

        val first = planFtpRebind(emptySet(), listOf(home))
        assertEquals(listOf(home), first.open)
        assertEquals(emptySet<String>(), first.close)

        val switched = planFtpRebind(setOf(home.ip), listOf(office))
        assertEquals(listOf(office), switched.open)
        assertEquals(setOf(home.ip), switched.close)

        val lost = planFtpRebind(setOf(office.ip), emptyList())
        assertEquals(emptyList<FtpBindTarget>(), lost.open)
        assertEquals(setOf(office.ip), lost.close)

        val added = planFtpRebind(setOf(office.ip), listOf(office, hotspot))
        assertEquals(listOf(hotspot), added.open)
        assertEquals(emptySet<String>(), added.close)

        val steady = planFtpRebind(setOf(office.ip), listOf(office))
        assertEquals(emptyList<FtpBindTarget>(), steady.open)
        assertEquals(emptySet<String>(), steady.close)
    }

    @Test
    fun pasvAdvertisesOnlyTheBoundControlAddress() {
        val bound = setOf("192.168.0.20", "10.82.121.117")
        assertEquals("192.168.0.20", resolvePasvAdvertisedHost("192.168.0.20", bound))
        assertEquals("10.82.121.117", resolvePasvAdvertisedHost("::ffff:10.82.121.117", bound))
        assertNull(resolvePasvAdvertisedHost("0.0.0.0", bound))
        assertNull(resolvePasvAdvertisedHost("::", bound))
        assertNull(resolvePasvAdvertisedHost("10.64.12.3", bound))
        assertNull(resolvePasvAdvertisedHost(null, bound))
        assertNull(resolvePasvAdvertisedHost("192.168.0.20", emptySet()))
    }

    @Test
    fun onlyAddressNotReadyIsRetriedSilently() {
        assertTrue(isRetryableBindError("bind failed: EADDRNOTAVAIL (Cannot assign requested address)"))
        assertTrue(isRetryableBindError("Cannot assign requested address"))
        assertFalse(isRetryableBindError("bind failed: EADDRINUSE (Address already in use)"))
        assertFalse(isRetryableBindError("bind failed: EACCES (Permission denied)"))
        assertFalse(isRetryableBindError(null))
    }

    @Test
    fun formatsBothHostsForTheInbox() {
        val binds = selectFtpBindTargets(
            listOf(
                FtpBindTarget("wlan0", "192.168.0.20"),
                FtpBindTarget("wlan1", "10.82.121.117"),
            ),
        )
        assertEquals("wlan0  192.168.0.20\nwlan1  10.82.121.117", formatFtpHosts(binds))
        assertEquals("wlan0  192.168.0.20 · wlan1  10.82.121.117", formatFtpHostsOneLine(binds))
        assertEquals("", formatFtpHosts(emptyList()))
    }

    @Test
    fun ingestibleUploadNamesAcceptStillFormats() {
        assertEquals(true, isIngestibleUploadName("DSC0001.JPG"))
        assertEquals(true, isIngestibleUploadName("shot.jpeg"))
        assertEquals(true, isIngestibleUploadName("frame.PNG"))
        assertEquals(true, isIngestibleUploadName("IMG_0001.heic"))
        assertEquals(true, isIngestibleUploadName("IMG_0001.HEIF"))
        assertEquals(true, isIngestibleUploadName("anim.webp"))
        assertEquals(true, isIngestibleUploadName("folder/nested/photo.jpg"))
    }

    @Test
    fun ingestibleUploadNamesRejectTempAndDotfiles() {
        assertEquals(false, isIngestibleUploadName(".hidden.jpg"))
        assertEquals(false, isIngestibleUploadName("foo.tmp"))
        assertEquals(false, isIngestibleUploadName("foo.JPG.tmp"))
        assertEquals(false, isIngestibleUploadName("foo.part"))
        assertEquals(false, isIngestibleUploadName("shot.heic.part"))
        assertEquals(false, isIngestibleUploadName("note.txt"))
        assertEquals(false, isIngestibleUploadName(""))
    }
}
