package io.github.wa_otomia.darkroom.core

import io.github.wa_otomia.darkroom.data.ftp.CameraFtpServer
import io.github.wa_otomia.darkroom.data.ftp.FtpHooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** The server binds only the addresses it is given, never the wildcard, and follows changes. */
class FtpBindServerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun canConnect(port: Int): Boolean = try {
        Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500) }
        true
    } catch (_: Exception) {
        false
    }

    private fun server(
        port: Int,
        targets: (FtpInterfacePolicy) -> List<FtpBindTarget>,
        bound: MutableList<FtpBindState>,
        hooks: FtpHooks = FtpHooks.NOOP,
        policy: () -> FtpInterfacePolicy = { FtpInterfacePolicy() },
    ) = CameraFtpServer(
        bindTargets = targets,
        port = port,
        user = "u",
        password = "p",
        pasvMin = 0,
        pasvMax = 0,
        inbox = tmp.newFolder(),
        onUploaded = { _, _ -> },
        hooks = hooks,
        onBoundChanged = { bound += it },
        interfacePolicy = policy,
    )

    @Test
    fun wildcardTargetIsNeverBound() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        val srv = server(
            port,
            {
                listOf(
                    FtpBindTarget("*", "0.0.0.0"),
                    FtpBindTarget("wlan0", "0.0.0.00"),
                    FtpBindTarget("wlan1", "000.000.000.000"),
                )
            },
            bound,
        )
        srv.allowLoopbackForTest()
        srv.start()
        try {
            assertFalse(srv.status.listening)
            assertFalse(canConnect(port))
            assertTrue(bound.isEmpty())
        } finally {
            srv.stop()
        }
    }

    @Test
    fun loopbackIsRefusedOutsideTests() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        val srv = server(port, { listOf(FtpBindTarget("lo", "127.0.0.1")) }, bound)
        srv.start()
        try {
            assertFalse(srv.status.listening)
            assertFalse(canConnect(port))
        } finally {
            srv.stop()
        }
    }

    @Test
    fun noAddressWaitsThenBindsAndUnbindsAsTheAddressComesAndGoes() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        var targets = emptyList<FtpBindTarget>()
        val srv = server(port, { targets }, bound)
        srv.allowLoopbackForTest()
        srv.start()
        try {
            assertFalse(srv.status.listening)
            assertFalse(canConnect(port))

            targets = listOf(FtpBindTarget("lo", "127.0.0.1"))
            srv.reconcile()
            assertTrue(srv.status.listening)
            assertEquals(FtpBindState(listOf(FtpBindTarget("lo", "127.0.0.1")), blocked = false), bound.last())
            assertTrue(canConnect(port))

            targets = emptyList()
            srv.reconcile()
            assertFalse(srv.status.listening)
            assertEquals(FtpBindState(), bound.last())
            assertFalse(canConnect(port))
        } finally {
            srv.stop()
        }
    }

    @Test
    fun portAlreadyTakenFailsTheStart() {
        ServerSocket().use { taken ->
            taken.reuseAddress = false
            taken.bind(InetSocketAddress("127.0.0.1", 0))
            val bound = mutableListOf<FtpBindState>()
            val srv = server(taken.localPort, { listOf(FtpBindTarget("lo", "127.0.0.1")) }, bound)
            srv.allowLoopbackForTest()
            val failed = try {
                srv.start()
                false
            } catch (_: IllegalStateException) {
                true
            }
            srv.stop()
            assertTrue(failed)
        }
    }

    @Test
    fun portTakenWhenWifiArrivesAfterStartIsReportedAsBlockedNotWaiting() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        var targets = emptyList<FtpBindTarget>()
        val srv = server(port, { targets }, bound)
        srv.allowLoopbackForTest()
        srv.start()
        ServerSocket().use { taken ->
            taken.reuseAddress = false
            taken.bind(InetSocketAddress("127.0.0.1", port))
            try {
                targets = listOf(FtpBindTarget("lo", "127.0.0.1"))
                srv.reconcile()
                assertEquals(FtpBindState(emptyList(), blocked = true), bound.last())
            } finally {
                taken.close()
            }
            srv.reconcile()
            assertEquals(FtpBindState(listOf(FtpBindTarget("lo", "127.0.0.1")), blocked = false), bound.last())
            srv.stop()
        }
    }

    @Test
    fun removingTheAddressDuringAnUploadFailsTheTransferAndDeletesThePartialFile() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        var targets = listOf(FtpBindTarget("lo", "127.0.0.1"))
        val failed = CopyOnWriteArrayList<String>()
        val started = CountDownLatch(1)
        val failedLatch = CountDownLatch(1)
        val hooks = object : FtpHooks by FtpHooks.NOOP {
            override fun transferStarted(sessionId: String, filename: String, total: Long?): String {
                started.countDown()
                return "t1"
            }
            override fun transferFailed(transferId: String, error: String) {
                failed += "$transferId:$error"
                failedLatch.countDown()
            }
        }
        val srv = server(port, { targets }, bound, hooks)
        srv.admitInterfaceForTest { true }
        srv.allowLoopbackForTest()
        srv.start()
        val inbox = tmp.root.listFiles()!!.single { it.isDirectory }
        try {
            Socket("127.0.0.1", port).use { control ->
                control.soTimeout = 5_000
                val reader = control.getInputStream().bufferedReader()
                val writer = control.getOutputStream().bufferedWriter()
                fun send(line: String) {
                    writer.write(line + "\r\n")
                    writer.flush()
                }
                assertTrue(reader.readLine().startsWith("220"))
                send("USER u"); assertTrue(reader.readLine().startsWith("331"))
                send("PASS p"); assertTrue(reader.readLine().startsWith("230"))
                send("EPSV")
                val epsv = reader.readLine()
                assertTrue(epsv, epsv.startsWith("229"))
                val dataPort = Regex("\\|\\|\\|(\\d+)\\|").find(epsv)!!.groupValues[1].toInt()
                send("STOR DSC0001.JPG")
                Socket("127.0.0.1", dataPort).use { data ->
                    assertTrue(reader.readLine().startsWith("150"))
                    data.getOutputStream().write(ByteArray(4096) { 1 })
                    data.getOutputStream().flush()
                    assertTrue(started.await(5, TimeUnit.SECONDS))
                    Thread.sleep(300)
                    targets = emptyList()
                    srv.reconcile()
                    assertTrue(failedLatch.await(5, TimeUnit.SECONDS))
                }
            }
            assertEquals(listOf("t1:文件不完整"), failed.toList())
            assertFalse(java.io.File(inbox, "DSC0001.JPG").exists())
            assertFalse(srv.status.listening)
        } finally {
            srv.stop()
        }
    }

    @Test
    fun aFailingInterfaceScanDoesNotFailTheStartOrDropABind() {
        val port = freePort()
        val bound = mutableListOf<FtpBindState>()
        var fail = true
        val srv = server(
            port,
            { if (fail) error("interface went away") else listOf(FtpBindTarget("lo", "127.0.0.1")) },
            bound,
        )
        srv.allowLoopbackForTest()
        srv.start()
        try {
            assertFalse(srv.status.listening)
            fail = false
            srv.reconcile()
            assertTrue(srv.status.listening)
            fail = true
            srv.reconcile()
            assertTrue(srv.status.listening)
            assertTrue(canConnect(port))
        } finally {
            srv.stop()
        }
    }

    @Test
    fun eachPassReadsThePolicyAndHandsItToTheTargetProvider() {
        val port = freePort()
        val seen = mutableListOf<FtpInterfacePolicy>()
        var current = FtpInterfacePolicy(tethered = setOf("ap0"))
        val srv = server(port, { p -> seen += p; emptyList() }, mutableListOf(), policy = { current })
        srv.start()
        try {
            current = FtpInterfacePolicy(denied = setOf("rmnet1"))
            srv.reconcile()
            assertEquals(
                listOf(FtpInterfacePolicy(tethered = setOf("ap0")), FtpInterfacePolicy(denied = setOf("rmnet1"))),
                seen.take(2),
            )
        } finally {
            srv.stop()
        }
    }
}
