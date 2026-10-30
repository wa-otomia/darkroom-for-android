package io.github.wa_otomia.darkroom.core

data class FtpBindTarget(
    val iface: String,
    val ip: String,
)

/**
 * Addresses the FTP server listens on: the IPv4 of every Wi-Fi interface that [isAllowedFtpInterface]
 * accepts (Wi-Fi connection, the phone's hotspot, a Wi-Fi Direct group it owns), nothing else.
 *
 * Mobile data, VPN, USB/Bluetooth tethering, Ethernet and loopback are dropped. There is no wildcard
 * fallback: an empty result means "no Wi-Fi", and the server then listens on nothing.
 */
fun selectFtpBindTargets(
    available: List<FtpBindTarget>,
    policy: FtpInterfacePolicy = FtpInterfacePolicy(),
): List<FtpBindTarget> =
    available
        .filter { isAllowedFtpInterface(it.iface, policy) && isFtpListenAddress(it.ip) }
        .distinctBy { it.ip }
        .sortedWith(compareBy({ it.iface.lowercase() }, { it.ip }))

/** One row per bound address; empty when nothing is bound, so the caller picks the "no Wi-Fi" text. */
fun formatFtpHosts(binds: List<FtpBindTarget>): String =
    binds.joinToString("\n") { "${it.iface}  ${it.ip}" }

fun formatFtpHostsOneLine(binds: List<FtpBindTarget>): String =
    formatFtpHosts(binds).replace("\n", " · ")

fun isUsableAdvertisedIpv4(ip: String): Boolean =
    IPV4.matches(ip) && ip != "0.0.0.0" && ip != "127.0.0.1"

/**
 * A specific IPv4 the server may bind. Never the wildcard (`0.0.0.0` would also listen on mobile
 * data) and never loopback, except when a host JVM test explicitly asks for it.
 */
fun isFtpListenAddress(ip: String, allowLoopback: Boolean = false): Boolean {
    if (!IPV4.matches(ip)) return false
    // Compare numbers, not text: "0.0.0.00" or "000.000.000.000" are the wildcard too.
    val octets = ip.split('.').map { it.toInt() }
    if (octets.any { it > 255 }) return false
    if (octets.all { it == 0 } || octets.all { it == 255 }) return false
    if (octets[0] == 127) return allowLoopback
    return true
}

/**
 * What the server is listening on. [bound] empty with [blocked] false means no Wi-Fi address yet;
 * [blocked] true means Wi-Fi addresses exist but none could be bound for a reason that retrying
 * will not fix by itself (port taken by another app, or a port the app may not use).
 */
data class FtpBindState(
    val bound: List<FtpBindTarget> = emptyList(),
    val blocked: Boolean = false,
)

/**
 * A bind failure worth retrying on the next poll: the address is not usable yet (it was just
 * assigned, or is going away). Anything else, such as the port being in use or not permitted,
 * needs the user to act.
 */
fun isRetryableBindError(message: String?): Boolean {
    val m = message.orEmpty()
    return m.contains("EADDRNOTAVAIL") || m.contains("Cannot assign requested address", ignoreCase = true)
}

/** Listeners to open and to close so the bound set matches [desired]. */
data class FtpRebindPlan(
    val open: List<FtpBindTarget>,
    val close: Set<String>,
)

fun planFtpRebind(bound: Set<String>, desired: List<FtpBindTarget>): FtpRebindPlan {
    val wanted = desired.map { it.ip }.toSet()
    return FtpRebindPlan(
        open = desired.filter { it.ip !in bound }.distinctBy { it.ip },
        close = bound - wanted,
    )
}

/**
 * Address a PASV reply advertises: the control connection's local address, and only when the server
 * is actually listening on it. The passive listener binds that same address, so the reply and the
 * socket always agree. Anything else (wildcard, IPv6, an address no longer bound) gives null and the
 * command fails with 425 instead of guessing another interface.
 */
fun resolvePasvAdvertisedHost(localHost: String?, bound: Collection<String>): String? {
    val local = localHost?.removePrefix("::ffff:") ?: return null
    return local.takeIf { IPV4.matches(it) && it != "0.0.0.0" && it in bound }
}

private val IPV4 = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
