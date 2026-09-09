package app.darkroom.android.data.ftp

import app.darkroom.android.core.FtpBindTarget
import app.darkroom.android.core.formatFtpHosts
import app.darkroom.android.core.formatFtpHostsOneLine
import app.darkroom.android.core.isIngestibleUploadName
import app.darkroom.android.core.resolvePasvAdvertisedHost
import app.darkroom.android.core.selectFtpBindTargets
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
import java.util.concurrent.Executors
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

class CameraFtpServer(
    private val bind: String,
    private val port: Int,
    private val user: String,
    private val password: String,
    private val pasvMin: Int,
    private val pasvMax: Int,
    private val inbox: File,
    private val onUploaded: (File, String) -> Unit,
    private val log: (String) -> Unit = {},
    private val hooks: FtpHooks = FtpHooks.NOOP,
) {
    constructor(
        bind: String,
        port: Int,
        user: String,
        password: String,
        pasvMin: Int,
        pasvMax: Int,
        inbox: File,
        onUploaded: (File) -> Unit,
        log: (String) -> Unit = {},
        hooks: FtpHooks = FtpHooks.NOOP,
    ) : this(bind, port, user, password, pasvMin, pasvMax, inbox, { file, _ -> onUploaded(file) }, log, hooks)
    private val running = AtomicBoolean(false)
    private val serverSockets = mutableListOf<ServerSocket>()
    private val pool = Executors.newCachedThreadPool()
    @Volatile var status: FtpListenStatus = FtpListenStatus(false, bind, port, inbox.absolutePath)
        private set

    fun start() {
        if (!running.compareAndSet(false, true)) return
        inbox.mkdirs()
        val bindIp = if (bind == "0.0.0.0" || bind == "*") "0.0.0.0" else bind
        val sock = ServerSocket()
        sock.reuseAddress = true
        try {
            sock.bind(InetSocketAddress(InetAddress.getByName(bindIp), port), 50)
        } catch (e: Exception) {
            running.set(false)
            try {
                sock.close()
            } catch (_: Exception) {
            }
            error("ftp bind failed on $bindIp")
        }
        serverSockets += sock
        val hosts = enumerateLanAddresses()
        val hostLabel = formatFtpHostsOneLine(selectFtpBindTargets(hosts, includeLoopback = false))
        status = FtpListenStatus(true, hostLabel, port, inbox.absolutePath)
        log("listen $bindIp:$port $hostLabel")
        thread(name = "ftp-accept", isDaemon = true) {
            while (running.get()) {
                try {
                    val client = sock.accept()
                    pool.execute { Session(client).run() }
                } catch (_: Exception) {
                    if (!running.get()) break
                }
            }
        }
    }

    fun stop() {
        running.set(false)
        serverSockets.forEach { sock ->
            try {
                sock.close()
            } catch (_: Exception) {
            }
        }
        serverSockets.clear()
        pool.shutdownNow()
        status = status.copy(listening = false)
        log("stop $port")
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
        private var pasvServer: ServerSocket? = null
        private var dataSocket: Socket? = null
        private var utf8 = true
        private var expectedSize: Long? = null
        private var lastTransferId: String? = null
        private val abortRequested = AtomicBoolean(false)

        fun run() {
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
            val ss = openPasvSocket()
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
            val ss = openPasvSocket()
            pasvServer = ss
            reply(229, "Entering Extended Passive Mode (|||${ss.localPort}|)")
        }

        private fun openPasvSocket(): ServerSocket {
            val local = socket.localAddress ?: InetAddress.getByName("0.0.0.0")
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
                ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))
            }
        }

        private fun advertisedHost(): String = resolvePasvAdvertisedHost(
            localHost = socket.localAddress?.hostAddress,
            clientHost = socket.inetAddress?.hostAddress,
            lan = enumerateLanAddresses(),
            fallbackLan = firstLanIPv4(),
        )

        private fun acceptData(): Socket? {
            val ss = pasvServer ?: return null
            ss.soTimeout = 20_000
            return try {
                val s = ss.accept()
                dataSocket = s
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
            val received = AtomicLong(0)
            val copy = pool.submit {
                try {
                    FileOutputStream(dest, append).use { out ->
                        val dataIn = data.getInputStream()
                        val buf = ByteArray(STOR_CHUNK)
                        var lastAt = 0L
                        var lastBytes = 0L
                        while (!abortRequested.get()) {
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
            } finally {
                socket.soTimeout = previousTimeout
            }
            try {
                copy.get()
            } catch (_: Exception) {
                abortRequested.set(true)
            }
            val got = received.get()
            hooks.sessionActivity(sessionId, dest.name, got)
            val incomplete = abortRequested.get() || (total != null && got != total)
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
            lastTransferId = null
            dest.delete()
            if (transferId.isNotEmpty()) hooks.transferFailed(transferId, "文件不完整")
            log("STOR ${dest.name} failed: incomplete")
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

        fun enumerateLanAddresses(): List<FtpBindTarget> {
            val out = ArrayList<FtpBindTarget>()
            for (ni in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp || ni.isLoopback) continue
                val name = ni.name
                for (addr in Collections.list(ni.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip == "0.0.0.0") continue
                        out += FtpBindTarget(name, ip)
                    }
                }
            }
            return out
        }

        /** Wrapper kept so I/O UI can keep calling [wlanHostTargets] / [wlanHostOneLine]. */
        fun enumerateWlanAddresses(): List<FtpBindTarget> = enumerateLanAddresses()

        fun wlanHostTargets(): List<FtpBindTarget> =
            selectFtpBindTargets(enumerateLanAddresses(), includeLoopback = false)

        fun wlanHostLabel(): String = formatFtpHosts(wlanHostTargets())

        fun wlanHostOneLine(): String = formatFtpHostsOneLine(wlanHostTargets())

        fun firstLanIPv4(): String? = enumerateLanAddresses().minByOrNull { it.iface.lowercase() }?.ip

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
