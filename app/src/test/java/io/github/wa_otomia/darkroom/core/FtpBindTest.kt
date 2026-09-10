package io.github.wa_otomia.darkroom.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FtpBindTest {
    @Test
    fun bindsLanIpv4PlusLoopback() {
        val got = selectFtpBindTargets(
            listOf(
                FtpBindTarget("rmnet1", "100.1.2.3"),
                FtpBindTarget("wlan1", "10.82.121.117"),
                FtpBindTarget("wlan0", "192.168.0.20"),
            ),
        )
        assertEquals(
            listOf(
                FtpBindTarget("rmnet1", "100.1.2.3"),
                FtpBindTarget("wlan0", "192.168.0.20"),
                FtpBindTarget("wlan1", "10.82.121.117"),
                FtpBindTarget("lo", "127.0.0.1"),
            ),
            got,
        )
    }

    @Test
    fun fallsBackWhenNoLanAddresses() {
        assertEquals(
            listOf(FtpBindTarget("*", "0.0.0.0")),
            selectFtpBindTargets(emptyList()),
        )
    }

    @Test
    fun includesHotspotAndTetherInterfaces() {
        val got = selectFtpBindTargets(
            listOf(
                FtpBindTarget("ap0", "192.168.43.1"),
                FtpBindTarget("swlan0", "192.168.49.1"),
                FtpBindTarget("rndis0", "192.168.42.129"),
            ),
            includeLoopback = false,
        )
        assertEquals(
            listOf(
                FtpBindTarget("ap0", "192.168.43.1"),
                FtpBindTarget("rndis0", "192.168.42.129"),
                FtpBindTarget("swlan0", "192.168.49.1"),
            ),
            got,
        )
    }

    @Test
    fun pasvAdvertisesControlLocalThenClientSubnet() {
        val lan = listOf(
            FtpBindTarget("wlan0", "192.168.0.20"),
            FtpBindTarget("ap0", "192.168.43.1"),
        )
        assertEquals("192.168.43.1", resolvePasvAdvertisedHost("192.168.43.1", "192.168.43.50", lan))
        assertEquals("192.168.43.1", resolvePasvAdvertisedHost("0.0.0.0", "192.168.43.50", lan))
        assertEquals("192.168.0.20", resolvePasvAdvertisedHost("::", "10.1.2.3", lan, fallbackLan = "192.168.0.20"))
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
