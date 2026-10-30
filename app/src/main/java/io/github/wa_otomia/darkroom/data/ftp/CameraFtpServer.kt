package io.github.wa_otomia.darkroom.data.ftp

import io.github.wa_otomia.darkroom.core.FtpBindState
import io.github.wa_otomia.darkroom.core.FtpBindTarget
import io.github.wa_otomia.darkroom.core.FtpInterfacePolicy
import io.github.wa_otomia.darkroom.core.formatFtpHosts
import io.github.wa_otomia.darkroom.core.formatFtpHostsOneLine
import io.github.wa_otomia.darkroom.core.isAllowedFtpInterface
import io.github.wa_otomia.darkroom.core.isFtpListenAddress
import io.github.wa_otomia.darkroom.core.isIngestibleUploadName
import io.github.wa_otomia.darkroom.core.isRetryableBindError
import io.github.wa_otomia.darkroom.core.planFtpRebind
import io.github.wa_otomia.darkroom.core.resolvePasvAdvertisedHost
import io.github.wa_otomia.darkroom.core.selectFtpBindTargets
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

interface FtpHooks {
    fun sessionOpened(id: String, remote: String)
    fun sessionAuthed(id: String, user: String)
    fun sessionClosed(id: String)
    fun transferStarted(sessionId: String, filename: String, total: Long?): String
    fun ingestStarted(sessionId: String, filename: String, bytes: Long): String
    fun sessionActivity(sessionId: String, filename: String, bytes: Long)
    fun transferProgress(transferId: String, bytes: Long, total: Long?)
    fun transferFinished(transferId: String)
    fun transferFailed(transferId: String, error: String)

    companion object {
        val NOOP: FtpHooks = object : FtpHooks {
            override fun sessionOpened(id: String, remote: String) = Unit
            override fun sessionAuthed(id: String, user: String) = Unit
            override fun sessionClosed(id: String) = Unit
            override fun transferStarted(sessionId: String, filename: String, total: Long?) = ""
            override fun ingestStarted(sessionId: String, filename: String, bytes: Long) = ""
            override fun sessionActivity(sessionId: String, filename: String, bytes: Long) = Unit
            override fun transferProgress(transferId: String, bytes: Long, total: Long?) = Unit
            override fun transferFinished(transferId: String) = Unit
            override fun transferFailed(transferId: String, error: String) = Unit
        }
    }
}

data class FtpListenStatus(
    val listening: Boolean,
    val bind: String,
    val port: Int,
    val inbox: String,
)

/**
 * Camera FTP server. It never binds the wildcard address: [interfacePolicy] and then [bindTargets]
 * are read at [start] and every [REBIND_POLL_MS], and one listener is kept per returned address
 * (production passes the Wi-Fi and phone-hotspot IPv4 addresses, so no listener is ever opened on
 * a mobile-data address). The same policy snapshot is re-checked for each accepted connection by
 * the interface owning its local address. Binding is by address, not by link: only a party able to
 * route this phone's private Wi-Fi address over its mobile-data link (the carrier side) could reach
 * the listener that way, and it would still need the FTP login. An empty list means no Wi-Fi: the
 * server stays running with no listener and binds as soon as Wi-Fi comes back. [onBoundChanged]
 * reports the addresses actually bound, and whether binding is blocked, after each change. Port, credentials and the PASV range are
 * fixed at construction; only the address set is re-read while running.
 */
class CameraFtpServer(
    private val bindTargets: (FtpInterfacePolicy) -> List<FtpBindTarget>,
    private val port: Int,
    private val user: String,
    private val password: String,
    private val pasvMin: Int,
    private val pasvMax: Int,
    private val inbox: File,
    private val onUploaded: (File, String) -> Unit,
    private val log: (String) -> Unit = {},
    private val hooks: FtpHooks = FtpHooks.NOOP,
    private val onBoundChanged: (FtpBindState) -> Unit = {},
    private val interfacePolicy: () -> FtpInterfacePolicy = { FtpInterfacePolicy() },
) {
    constructor(
        bindTargets: (FtpInterfacePolicy) -> List<FtpBindTarget>,
        port: Int,
        user: String,
        password: String,
        pasvMin: Int,
        pasvMax: Int,
        inbox: File,
        onUploaded: (File) -> Unit,
        log: (String) -> Unit = {},
        hooks: FtpHooks = FtpHooks.NOOP,
    ) : this(bindTargets, port, user, password, pasvMin, pasvMax, inbox, { file, _ -> onUploaded(file) }, log, hooks)
    private val running = AtomicBoolean(false)
    /** One listener per bound IPv4. Changed only under the instance lock (reconcile / stop). */
    private val listeners = LinkedHashMap<String, ServerSocket>()
    private var boundTargets: List<FtpBindTarget> = emptyList()
    /** Last bind failure reason per address, so each new reason is logged once. */
    private val failedIps = HashMap<String, String>()
    private var bindState = FtpBindState()
    /** Last bind error that retrying will not fix (port taken or not permitted), this pass. */
    private var fatalBindError: String? = null
    private var rebindThread: Thread? = null
    private val pool = Executors.newCachedThreadPool()
    private val sessions = ConcurrentHashMap.newKeySet<Session>()
    /** Latest system view of hotspot and mobile-data interfaces, refreshed on every pass. */
    @Volatile private var policy = FtpInterfacePolicy()
    private var loggedPolicy: FtpInterfacePolicy? = null
    @Volatile private var admitInterface: (String?) -> Boolean = { isAllowedFtpInterface(it, policy) }
    @Volatile private var allowLoopback = false
    @Volatile private var boundIps: Set<String> = emptySet()
    @Volatile var status: FtpListenStatus = FtpListenStatus(false, "", port, inbox.absolutePath)
        private set

    /**
     * Binds the current targets and starts watching for address changes. Throws when no address
     * could be bound because of the port itself (in use, or not permitted such as 21). No address at
     * all, or one that is not bindable yet, is not an error: the server waits and binds on a later
     * poll. A port conflict that appears only later (Wi-Fi joined after start) is reported as
     * [FtpBindState.blocked] and retried, since the other app may let go of the port.
     */
    fun start() {
        if (!running.compareAndSet(false, true)) return
        inbox.mkdirs()
        val wanted = try {
            reconcile()
        } catch (e: Exception) {
            stop()
            throw e
        }
        val fatal = fatalBindError
        if (wanted > 0 && boundIps.isEmpty() && fatal != null) {
            stop()
            error("ftp bind failed on port $port: $fatal")
        }
        rebindThread = thread(name = "ftp-rebind", isDaemon = true) {
            while (running.get()) {
                try {
                    Thread.sleep(REBIND_POLL_MS)
                } catch (_: InterruptedException) {
                    break
                }
                try {
                    reconcile()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Brings the listeners in line with [bindTargets]: closes the ones whose address is gone (and
     * aborts sessions on them, so an upload in flight fails instead of hanging), opens new ones.
     * Returns how many addresses were wanted.
     */
    @Synchronized
    internal fun reconcile(): Int {
        if (!running.get()) return 0
        // An interface vanishing mid-enumeration throws; keep the current listeners and look again
        // on the next poll rather than failing the start or dropping a working bind.
        val offered = try {
            policy = interfacePolicy()
            if (policy != loggedPolicy) {
                // Names only: lets the activity log explain why a hotspot was or was not served.
                loggedPolicy = policy
                log("interfaces hotspot=${policy.tethered.sorted()} mobile/vpn=${policy.denied.sorted()}")
            }
            bindTargets(policy)
        } catch (e: Exception) {
            log("interface scan failed: ${e.message ?: e.javaClass.simpleName}")
            return 0
        }
        val desired = offered.filter { isFtpListenAddress(it.ip, allowLoopback) }.distinctBy { it.ip }
        val plan = planFtpRebind(listeners.keys.toSet(), desired)
        fatalBindError = null
        for (ip in plan.close) {
            closeQuietly(listeners.remove(ip))
            sessions.filter { it.localIp == ip }.forEach { it.abortForUnbind() }
            log("unbind $ip:$port")
        }
        failedIps.keys.retainAll(desired.map { it.ip }.toSet())
        for (target in plan.open) {
            // Check the parsed address too, not just the string: "0.0.0.00" also means the wildcard.
            val address = try {
                InetAddress.getByName(target.ip)
            } catch (_: Exception) {
                null
            }
            if (address !is Inet4Address || address.isAnyLocalAddress ||
                (address.isLoopbackAddress && !allowLoopback)
            ) {
                continue
            }
            val sock = ServerSocket()
            try {
                sock.reuseAddress = true
                sock.bind(InetSocketAddress(address, port), 50)
            } catch (e: Exception) {
                closeQuietly(sock)
                val reason = e.message ?: e.javaClass.simpleName
                if (!isRetryableBindError(reason)) fatalBindError = reason
                // Retried on the next poll; each new reason is logged once, not every few seconds.
                if (failedIps.put(target.ip, reason) != reason) {
                    log("bind failed ${target.iface} ${target.ip}:$port: $reason")
                }
                continue
            }
            failedIps.remove(target.ip)
            listeners[target.ip] = sock
            startAcceptLoop(sock)
        }
        val now = desired.filter { it.ip in listeners }
        val next = FtpBindState(bound = now, blocked = now.isEmpty() && fatalBindError != null)
        if (now != boundTargets) {
            boundTargets = now
            boundIps = now.map { it.ip }.toSet()
            val label = formatFtpHostsOneLine(now)
            status = FtpListenStatus(now.isNotEmpty(), label, port, inbox.absolutePath)
            log(if (now.isEmpty()) "waiting for wlan on port $port" else "listen $label port $port")
        }
        if (next != bindState) {
            bindState = next
            onBoundChanged(next)
        }
        return desired.size
    }

    private fun startAcceptLoop(sock: ServerSocket) {
        thread(name = "ftp-accept", isDaemon = true) {
            while (running.get() && !sock.isClosed) {
                var client: Socket? = null
                try {
                    client = sock.accept()
                    if (!admitSocket(client)) continue
                    // Registered before it runs, so an unbind of this address can always find it.
                    // reconcile closes the listener before scanning sessions; a session added after
                    // that scan sees the closed listener here and is aborted instead.
                    val session = Session(client)
                    sessions.add(session)
                    if (sock.isClosed || !running.get()) {
                        sessions.remove(session)
                        client.close()
                        continue
                    }
                    try {
                        pool.execute { session.run() }
                    } catch (e: RejectedExecutionException) {
                        sessions.remove(session)
                        throw e
                    }
                } catch (_: RejectedExecutionException) {
                    try {
                        client?.close()
                    } catch (_: Exception) {
                    }
                    break
                } catch (_: Exception) {
                    try {
                        if (client != null && !client.isClosed && sessions.none { it.owns(client) }) client.close()
                    } catch (_: Exception) {
                    }
                    if (!running.get() || sock.isClosed) break
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        running.set(false)
        rebindThread?.interrupt()
        rebindThread = null
        sessions.forEach { it.abortForStop() }
        listeners.values.forEach { closeQuietly(it) }
        listeners.clear()
        boundTargets = emptyList()
        boundIps = emptySet()
        bindState = FtpBindState()
        pool.shutdownNow()
        status = status.copy(listening = false)
        log("stop $port")
    }

    private fun closeQuietly(sock: ServerSocket?) {
        try {
            sock?.close()
        } catch (_: Exception) {
        }
    }

    /**
     * Test-only. Production admits Wi-Fi and hotspot interfaces only; host JVM tests have none.
     * Refuses to widen admission after [start].
     */
    internal fun admitInterfaceForTest(predicate: (String?) -> Boolean) {
        check(!running.get()) { "interface admission is fixed once listening" }
        admitInterface = predicate
    }

    /** Test-only: let [bindTargets] name a loopback address. The wildcard is never allowed. */
    internal fun allowLoopbackForTest() {
        check(!running.get()) { "bind rules are fixed once listening" }
        allowLoopback = true
    }

    private fun admitSocket(socket: Socket): Boolean {
        val iface = try {
            NetworkInterface.getByInetAddress(socket.localAddress)?.name
        } catch (_: Exception) {
            null
        }
        if (admitInterface(iface)) return true
        val remote = socket.inetAddress?.hostAddress ?: "unknown"
        log("rejected $remote via ${iface ?: "unknown"}")
        try {
            socket.close()
        } catch (_: Exception) {
        }
        return false
    }

    private inner class Session(private val socket: Socket) {
        private val input = BufferedInputStream(socket.getInputStream())
        private val output = BufferedOutputStream(socket.getOutputStream())
        private val sessionId = UUID.randomUUID().toString()
        private var authed = false
        private var pendingUser = ""
        private var cwd = "/"
        private var renameFrom: File? = null
        private var typeImage = true
        @Volatile private var pasvServer: ServerSocket? = null
        @Volatile private var dataSocket: Socket? = null
        private var utf8 = true
        private var expectedSize: Long? = null
        private var lastTransferId: String? = null
        private val abortRequested = AtomicBoolean(false)
        val localIp: String? = socket.localAddress?.hostAddress?.removePrefix("::ffff:")

        fun owns(other: Socket): Boolean = other === socket

        fun run() {
            sessions.add(this)
            try {
                val remote = socket.inetAddress?.hostAddress
                    ?: socket.remoteSocketAddress?.toString()
                    ?: "unknown"
                hooks.sessionOpened(sessionId, remote)
                try {
                    applyIdleTimeout()
                    reply(220, "darkroom ready")
                    while (running.get()) {
                        val line = readLine() ?: break
                        if (line.isBlank()) continue
                        handle(line)
                    }
                } catch (_: Exception) {
                } finally {
                    abortRequested.set(true)
                    hooks.sessionClosed(sessionId)
                    closeData()
                    try {
                        socket.close()
                    } catch (_: Exception) {
                    }
                }
            } finally {
                sessions.remove(this)
            }
        }

        fun abortForStop() {
            abortRequested.set(true)
            closeData()
        }

        /**
         * The address this session came in on is no longer served (Wi-Fi changed or dropped).
         * Only the read side is shut: the session thread then reads end-of-stream, fails a STOR
         * in flight through failStor (transfer marked failed, partial file deleted) and closes
         * the socket itself.
         */
        fun abortForUnbind() {
            abortRequested.set(true)
            closeData()
            try {
                socket.shutdownInput()
            } catch (_: Exception) {
                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }

        private fun applyIdleTimeout() {
            socket.soTimeout = if (authed) AUTHED_IDLE_MS else PREAUTH_IDLE_MS
        }

        private fun handle(line: String) {
            val space = line.indexOf(' ')
            val cmd = (if (space < 0) line else line.substring(0, space)).uppercase(Locale.US)
            val arg = if (space < 0) "" else line.substring(space + 1).trim()
            when (cmd) {
                "USER" -> {
                    pendingUser = arg
                    reply(331, "password required")
                }
                "PASS" -> {
                    if (secretEqual(pendingUser, user) && secretEqual(arg, password)) {
                        authed = true
                        applyIdleTimeout()
                        hooks.sessionAuthed(sessionId, pendingUser)
                        log("login $user")
                        reply(230, "logged in")
                    } else {
                        authed = false
                        applyIdleTimeout()
                        log("login rejected")
                        reply(530, "invalid username or password")
                    }
                }
                "QUIT" -> {
                    reply(221, "bye")
                    socket.close()
                }
                "NOOP" -> reply(200, "ok")
                "ALLO" -> {
                    expectedSize = arg.split(Regex("\\s+")).firstOrNull()?.toLongOrNull()
                    reply(200, "ok")
                }
                "SYST" -> reply(215, "UNIX Type: L8")
                "FEAT" -> {
                    writeRaw("211-Features\r\n UTF8\r\n SIZE\r\n MDTM\r\n PASV\r\n EPSV\r\n REST STREAM\r\n211 End\r\n")
                }
                "OPTS" -> {
                    utf8 = arg.uppercase(Locale.US).contains("UTF8")
                    reply(200, "UTF8 OK")
                }
                "TYPE" -> {
                    typeImage = !arg.uppercase(Locale.US).startsWith("A")
                    reply(200, "type set")
                }
                "MODE", "STRU" -> reply(200, "ok")
                "PWD", "XPWD" -> reply(257, "\"${virtualPath(cwd)}\" is current directory")
                "CWD", "XCWD" -> {
                    if (!requireAuth()) return
                    val next = resolveVirtual(arg)
                    val dir = fileFor(next)
                    if (dir.isDirectory || next == "/") {
                        cwd = next
                        reply(250, "cwd ok")
                    } else {
                        reply(550, "no such directory")
                    }
                }
                "CDUP" -> {
                    cwd = parentVirtual(cwd)
                    reply(250, "cdup ok")
                }
                "MKD", "XMKD" -> {
                    if (!requireAuth()) return
                    val dir = fileFor(resolveVirtual(arg))
                    if (dir.mkdirs() || dir.isDirectory) reply(257, "\"$arg\" created") else reply(550, "mkdir failed")
                }
                "RMD", "XRMD" -> {
                    if (!requireAuth()) return
                    val dir = fileFor(resolveVirtual(arg))
                    if (dir.isDirectory && dir.delete()) reply(250, "removed") else reply(550, "rmdir failed")
                }
                "DELE" -> {
                    if (!requireAuth()) return
                    val file = fileFor(resolveVirtual(arg))
                    if (file.isFile && file.delete()) reply(250, "deleted") else reply(550, "delete failed")
                }
                "SIZE" -> {
                    if (!requireAuth()) return
                    val file = fileFor(resolveVirtual(arg))
                    if (file.isFile) reply(213, file.length().toString()) else reply(550, "not found")
                }
                "MDTM" -> {
                    if (!requireAuth()) return
                    val file = fileFor(resolveVirtual(arg))
                    if (file.isFile) reply(213, "19700101000000") else reply(550, "not found")
                }
                "PASV" -> pasv()
                "EPSV" -> epsv()
                "PORT" -> reply(502, "use PASV")
                "LIST", "NLST" -> list(cmd == "NLST", arg)
                "RETR" -> retr(arg)
                "STOR", "APPE" -> stor(arg, append = cmd == "APPE")
                "ABOR" -> {
                    abortRequested.set(true)
                    closeData()
                    reply(226, "aborted")
                }
                "RNFR" -> {
                    if (!requireAuth()) return
                    val file = fileFor(resolveVirtual(arg))
                    renameFrom = file
                    reply(350, "ready for RNTO")
                }
                "RNTO" -> rnto(arg)
                "REST" -> reply(350, "restart ignored")
                "AUTH" -> reply(502, "TLS not used")
                "PBSZ", "PROT" -> reply(200, "ok")
                "HELP" -> reply(214, "USER PASS PASV STOR RNTO LIST")
                else -> reply(502, "command not implemented")
            }
        }

        private fun requireAuth(): Boolean {
            if (authed) return true
            reply(530, "please login")
            return false
        }

        private fun pasv() {
            if (!requireAuth()) return
            closeData()
            val local = advertisedHost()
            val ss = local?.let { openPasvSocket(it) }
            if (local == null || ss == null) {
                reply(425, "cannot open passive data connection")
                return
            }
            pasvServer = ss
            val p = ss.localPort
            val p1 = p / 256
            val p2 = p % 256
            val host = local.replace('.', ',')
            reply(227, "Entering Passive Mode ($host,$p1,$p2)")
        }

        private fun epsv() {
            if (!requireAuth()) return
            closeData()
            val ss = advertisedHost()?.let { openPasvSocket(it) }
            if (ss == null) {
                reply(425, "cannot open passive data connection")
                return
            }
            pasvServer = ss
            reply(229, "Entering Extended Passive Mode (|||${ss.localPort}|)")
        }

        /**
         * Passive listener on [host], the bound Wi-Fi address the control connection reached
         * (resolved once by [advertisedHost], so a racing unbind cannot turn it into the wildcard).
         * Null makes the command fail with 425.
         */
        private fun openPasvSocket(host: String): ServerSocket? {
            val local = try {
                InetAddress.getByName(host)
            } catch (_: Exception) {
                return null
            }
            if (local.isAnyLocalAddress) return null
            if (pasvMin in 1..65535 && pasvMax >= pasvMin) {
                for (p in pasvMin..pasvMax) {
                    try {
                        return ServerSocket(p, 1, local)
                    } catch (_: Exception) {
                    }
                }
            }
            return try {
                ServerSocket(0, 1, local)
            } catch (_: Exception) {
                null
            }
        }

        private fun advertisedHost(): String? {
            val local = socket.localAddress ?: return null
            if (local.isAnyLocalAddress) return null
            return resolvePasvAdvertisedHost(localHost = local.hostAddress, bound = boundIps)
        }

        private fun acceptData(): Socket? {
            val ss = pasvServer ?: return null
            ss.soTimeout = 20_000
            return try {
                val s = ss.accept()
                if (!admitSocket(s)) return null
                dataSocket = s
                // ABOR and incomplete STOR leave abortRequested set. stop() clears running first.
                if (!running.get()) {
                    closeData()
                    return null
                }
                s
            } catch (_: SocketTimeoutException) {
                null
            } finally {
                try {
                    ss.close()
                } catch (_: Exception) {
                }
                pasvServer = null
            }
        }

        private fun list(namesOnly: Boolean, arg: String) {
            if (!requireAuth()) return
            val data = acceptData()
            if (data == null) {
                reply(425, "cannot open data connection")
                return
            }
            reply(150, "here comes the directory listing")
            try {
                val dir = fileFor(if (arg.isBlank()) cwd else resolveVirtual(arg))
                val listing = if (dir.isDirectory) dir.listFiles()?.sortedBy { it.name } ?: emptyList() else emptyList()
                val text = buildString {
                    for (f in listing) {
                        if (namesOnly) {
                            append(f.name).append("\r\n")
                        } else {
                            val kind = if (f.isDirectory) "d" else "-"
                            append("${kind}rw-r--r-- 1 camera camera ${f.length().toString().padStart(12)} Jan 01 00:00 ${f.name}\r\n")
                        }
                    }
                }
                data.getOutputStream().write(text.toByteArray(if (utf8) StandardCharsets.UTF_8 else StandardCharsets.ISO_8859_1))
                data.getOutputStream().flush()
                reply(226, "directory send OK")
            } catch (_: Exception) {
                reply(426, "transfer aborted")
            } finally {
                try {
                    data.close()
                } catch (_: Exception) {
                }
                dataSocket = null
            }
        }

        private fun retr(arg: String) {
            if (!requireAuth()) return
            val file = fileFor(resolveVirtual(arg))
            if (!file.isFile) {
                reply(550, "not found")
                return
            }
            val data = acceptData()
            if (data == null) {
                reply(425, "cannot open data connection")
                return
            }
            reply(150, "opening BINARY mode data connection")
            try {
                FileInputStream(file).use { it.copyTo(data.getOutputStream()) }
                data.getOutputStream().flush()
                reply(226, "transfer complete")
            } catch (_: Exception) {
                reply(426, "transfer aborted")
            } finally {
                try {
                    data.close()
                } catch (_: Exception) {
                }
                dataSocket = null
            }
        }

        private fun stor(arg: String, append: Boolean) {
            if (!requireAuth()) return
            val dest = fileFor(resolveVirtual(arg))
            dest.parentFile?.mkdirs()
            val data = acceptData()
            if (data == null) {
                reply(425, "cannot open data connection")
                return
            }
            reply(150, "ok to send data")
            val total = expectedSize
            expectedSize = null
            abortRequested.set(false)
            val ingestible = isIngestibleUploadName(dest.name)
            val transferId = if (ingestible) {
                hooks.transferStarted(sessionId, dest.name, total)
            } else {
                ""
            }
            lastTransferId = transferId.takeIf { it.isNotEmpty() }
            if (!running.get()) {
                failStor(dest, transferId)
                return
            }
            val received = AtomicLong(0)
            val copy = try {
                pool.submit {
                    try {
                        if (!running.get() || abortRequested.get()) return@submit
                        FileOutputStream(dest, append).use { out ->
                            val dataIn = data.getInputStream()
                            val buf = ByteArray(STOR_CHUNK)
                            var lastAt = 0L
                            var lastBytes = 0L
                            while (running.get() && !abortRequested.get()) {
                                val n = dataIn.read(buf)
                                if (n < 0) break
                                if (n == 0) continue
                                out.write(buf, 0, n)
                                val got = received.addAndGet(n.toLong())
                                val now = System.currentTimeMillis()
                                if (now - lastAt >= PROGRESS_MIN_MS || got - lastBytes >= PROGRESS_MIN_BYTES) {
                                    hooks.sessionActivity(sessionId, dest.name, got)
                                    if (transferId.isNotEmpty()) hooks.transferProgress(transferId, got, total)
                                    lastAt = now
                                    lastBytes = got
                                }
                            }
                        }
                    } finally {
                        try {
                            data.close()
                        } catch (_: Exception) {
                        }
                        dataSocket = null
                    }
                }
            } catch (_: RejectedExecutionException) {
                failStor(dest, transferId)
                return
            }
            val previousTimeout = socket.soTimeout
            socket.soTimeout = STOR_WATCH_MS
            try {
                while (!copy.isDone) {
                    try {
                        val line = readLine()
                        if (line == null) {
                            abortRequested.set(true)
                            closeData()
                            break
                        }
                        if (line.isBlank()) continue
                        val cmd = line.substringBefore(' ').uppercase(Locale.US)
                        if (cmd == "ABOR") {
                            abortRequested.set(true)
                            closeData()
                            break
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            } catch (e: Exception) {
                val stopped = Thread.currentThread().isInterrupted ||
                    e is InterruptedException ||
                    e is java.io.InterruptedIOException
                if (!stopped) throw e
                Thread.interrupted()
                abortRequested.set(true)
                closeData()
            } finally {
                try {
                    socket.soTimeout = previousTimeout
                } catch (_: Exception) {
                }
            }
            try {
                copy.get()
            } catch (_: Exception) {
                Thread.interrupted()
                abortRequested.set(true)
            }
            val got = received.get()
            hooks.sessionActivity(sessionId, dest.name, got)
            val incomplete = !running.get() || abortRequested.get() || (total != null && got != total)
            if (incomplete) {
                failStor(dest, transferId)
                return
            }
            if (transferId.isNotEmpty()) {
                hooks.transferProgress(transferId, got, total)
                hooks.transferFinished(transferId)
            }
            reply(226, "transfer complete")
            log("STOR ${dest.name}")
            maybeIngest(dest, lastTransferId)
        }

        private fun failStor(dest: File, transferId: String) {
            abortRequested.set(true)
            closeData()
            lastTransferId = null
            dest.delete()
            val serverRunning = running.get()
            val reason = storFailureReason(serverRunning)
            if (transferId.isNotEmpty()) hooks.transferFailed(transferId, reason)
            log("STOR ${dest.name} failed: ${if (serverRunning) "incomplete" else "interrupted"}")
            reply(426, "transfer aborted")
        }

        private fun rnto(arg: String) {
            if (!requireAuth()) return
            val from = renameFrom
            renameFrom = null
            if (from == null) {
                reply(503, "RNFR first")
                return
            }
            val dest = fileFor(resolveVirtual(arg))
            dest.parentFile?.mkdirs()
            val ok = from.renameTo(dest) || (from.exists() && from.copyTo(dest, overwrite = true).also { from.delete() }.exists())
            if (ok) {
                reply(250, "rename ok")
                log("RNTO ${dest.name}")
                if (isIngestibleUploadName(dest.name)) {
                    val size = dest.length()
                    val id = hooks.ingestStarted(sessionId, dest.name, size)
                    lastTransferId = id.takeIf { it.isNotEmpty() }
                    maybeIngest(dest, lastTransferId)
                }
            } else {
                reply(550, "rename failed")
            }
        }

        private fun maybeIngest(file: File, transferId: String?) {
            if (!isIngestibleUploadName(file.name)) return
            val id = transferId?.takeIf { it.isNotEmpty() }
                ?: hooks.ingestStarted(sessionId, file.name, file.length())
            try {
                onUploaded(file, id)
            } catch (e: Exception) {
                log("ingest ${file.name} failed: ${e.message}")
                if (id.isNotEmpty()) hooks.transferFailed(id, e.message ?: "无法解码图片")
                file.delete()
            }
        }

        private fun virtualPath(path: String): String = if (path.startsWith("/")) path else "/$path"

        private fun parentVirtual(path: String): String {
            val clean = virtualPath(path).trimEnd('/')
            val idx = clean.lastIndexOf('/')
            return if (idx <= 0) "/" else clean.substring(0, idx)
        }

        private fun resolveVirtual(arg: String): String {
            val raw = if (arg.startsWith("/")) arg else {
                val base = if (cwd.endsWith("/")) cwd else "$cwd/"
                base + arg
            }
            val parts = mutableListOf<String>()
            for (seg in raw.split('/')) {
                when (seg) {
                    "", "." -> {}
                    ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                    else -> parts += seg
                }
            }
            return "/" + parts.joinToString("/")
        }

        private fun fileFor(virtual: String): File {
            val rel = virtual.trimStart('/').replace("..", "")
            return if (rel.isEmpty()) inbox else File(inbox, rel)
        }

        private fun reply(code: Int, message: String) {
            writeRaw("$code $message\r\n")
        }

        private fun writeRaw(text: String) {
            output.write(text.toByteArray(StandardCharsets.US_ASCII))
            output.flush()
        }

        private fun readLine(): String? {
            val buf = ByteArrayOutputStream()
            while (true) {
                val b = input.read()
                if (b < 0) return if (buf.size() == 0) null else buf.toString(StandardCharsets.UTF_8.name())
                if (b == '\n'.code) break
                if (b != '\r'.code) buf.write(b)
            }
            return buf.toString(StandardCharsets.UTF_8.name())
        }

        private fun closeData() {
            try {
                dataSocket?.close()
            } catch (_: Exception) {
            }
            try {
                pasvServer?.close()
            } catch (_: Exception) {
            }
            dataSocket = null
            pasvServer = null
        }
    }

    companion object {
        private const val STOR_CHUNK = 64 * 1024
        private const val PROGRESS_MIN_MS = 100L
        private const val PROGRESS_MIN_BYTES = 256 * 1024
        private const val PREAUTH_IDLE_MS = 30_000
        private const val AUTHED_IDLE_MS = 120_000
        private const val STOR_WATCH_MS = 250
        /** How often the bound addresses are re-checked against the Wi-Fi interfaces. */
        const val REBIND_POLL_MS = 3_000L

        fun enumerateLanAddresses(): List<FtpBindTarget> {
            val out = ArrayList<FtpBindTarget>()
            val all = NetworkInterface.getNetworkInterfaces() ?: return out
            for (ni in Collections.list(all)) {
                // One interface going away mid-scan must not hide the others.
                try {
                    if (!ni.isUp || ni.isLoopback) continue
                    val name = ni.name
                    for (addr in Collections.list(ni.inetAddresses)) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            if (ip == "0.0.0.0") continue
                            out += FtpBindTarget(name, ip)
                        }
                    }
                } catch (_: Exception) {
                }
            }
            return out
        }

        /** The Wi-Fi and phone-hotspot IPv4 addresses the server listens on; empty without Wi-Fi. */
        fun wifiHostTargets(policy: FtpInterfacePolicy = FtpInterfacePolicy()): List<FtpBindTarget> =
            selectFtpBindTargets(enumerateLanAddresses(), policy)

        fun wifiHostLabel(policy: FtpInterfacePolicy = FtpInterfacePolicy()): String =
            formatFtpHosts(wifiHostTargets(policy))

        fun secretEqual(left: String, right: String): Boolean {
            val a = left.toByteArray(StandardCharsets.UTF_8)
            val b = right.toByteArray(StandardCharsets.UTF_8)
            if (a.size != b.size) {
                MessageDigest.isEqual(a, a)
                return false
            }
            return MessageDigest.isEqual(a, b)
        }
    }
}

/** Running STOR fails as incomplete; a stopped server fails as an interrupted transfer. */
fun storFailureReason(serverRunning: Boolean): String =
    if (serverRunning) "文件不完整" else "传输中断"
