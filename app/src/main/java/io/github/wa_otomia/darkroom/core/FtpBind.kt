package io.github.wa_otomia.darkroom.core

data class FtpBindTarget(
    val iface: String,
    val ip: String,
)

fun selectFtpBindTargets(available: List<FtpBindTarget>, includeLoopback: Boolean = true): List<FtpBindTarget> {
    val lan = available
        .filter { isLanIpv4Target(it) }
        .sortedBy { it.iface.lowercase() }
    if (lan.isEmpty()) return listOf(FtpBindTarget("*", "0.0.0.0"))
    if (!includeLoopback) return lan
    return lan + FtpBindTarget("lo", "127.0.0.1")
}

fun formatFtpHosts(binds: List<FtpBindTarget>): String {
    val visible = binds.filter { it.ip != "127.0.0.1" && it.iface != "lo" }
    val rows = visible.ifEmpty { binds }
    return rows.joinToString("\n") { "${it.iface}  ${it.ip}" }.ifBlank { "—" }
}

fun formatFtpHostsOneLine(binds: List<FtpBindTarget>): String =
    formatFtpHosts(binds).replace("\n", " · ")

fun isUsableAdvertisedIpv4(ip: String): Boolean =
    IPV4.matches(ip) && ip != "0.0.0.0" && ip != "127.0.0.1"

fun ipv4SameSlash24(a: String, b: String): Boolean {
    val left = a.removePrefix("::ffff:").split('.')
    val right = b.removePrefix("::ffff:").split('.')
    if (left.size != 4 || right.size != 4) return false
    return left[0] == right[0] && left[1] == right[1] && left[2] == right[2]
}

/**
 * Address a PASV/EPSV reply should advertise.
 *
 * Prefer the control socket's local IPv4 (the interface the client actually
 * reached). A wildcard / unspecified local address falls back to the LAN
 * address on the client's /24, then [fallbackLan].
 */
fun resolvePasvAdvertisedHost(
    localHost: String?,
    clientHost: String?,
    lan: List<FtpBindTarget>,
    fallbackLan: String? = lan.filter(::isLanIpv4Target).minByOrNull { it.iface.lowercase() }?.ip,
): String {
    val local = localHost?.removePrefix("::ffff:")
    if (local != null && isUsableAdvertisedIpv4(local)) return local
    val client = clientHost?.removePrefix("::ffff:")
    if (client != null && IPV4.matches(client)) {
        lan.firstOrNull { isLanIpv4Target(it) && ipv4SameSlash24(it.ip, client) }?.ip?.let { return it }
    }
    return fallbackLan ?: "127.0.0.1"
}

private fun isLanIpv4Target(target: FtpBindTarget): Boolean {
    val iface = target.iface.lowercase()
    if (iface == "lo" || iface == "*") return false
    return IPV4.matches(target.ip) && target.ip != "0.0.0.0" && target.ip != "127.0.0.1"
}

private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
