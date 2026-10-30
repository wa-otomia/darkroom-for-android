package io.github.wa_otomia.darkroom.core

/**
 * What the system currently reports about interfaces, read at run time.
 *
 * [tethered] are the interfaces the phone is sharing its connection on (its hotspot), so a vendor
 * hotspot name missing from the built-in list still works while it is on. [denied] are the
 * interfaces of networks the system marks as mobile data or VPN; they are refused whatever their name.
 */
data class FtpInterfacePolicy(
    val tethered: Set<String> = emptySet(),
    val denied: Set<String> = emptySet(),
)

/**
 * The camera FTP server serves Wi-Fi only, never mobile data. Wi-Fi means the phone's own Wi-Fi
 * connection and the networks it hosts for a camera:
 * - Wi-Fi connection: `wlan0`, and `wlan1`/`wlan2` for a second connection;
 * - hotspot: `wlan1`/`wlan2` (Pixel and AOSP-style phones), `swlan0` (Samsung), `ap0` (MediaTek
 *   phones from most brands), `softap0` (some Qualcomm builds), and the dual-band bridge
 *   `ap_br_wlan1` / `ap_br_ap0` / `ap_br_softap0`;
 * - a Wi-Fi Direct group the phone owns: `p2p0`, `p2p-wlan0-0`, `p2p-p2p0-1`;
 * - any other interface the system reports as the phone's hotspot ([FtpInterfacePolicy.tethered]).
 *
 * Mobile data (including CLAT `v4-*`), VPN, USB and Bluetooth tethering, Ethernet and loopback are
 * refused by name first, then anything in [FtpInterfacePolicy.denied]. Used to choose the addresses
 * to listen on, so no listener is ever opened on a mobile-data address. It is also applied to each
 * accepted connection by the interface that owns its local address; that check cannot see which
 * link a packet actually arrived on (Android, like Linux, delivers a packet for a local address
 * whatever interface it came in on), so it is a consistency check, not a per-link firewall.
 */
fun isAllowedFtpInterface(name: String?, policy: FtpInterfacePolicy = FtpInterfacePolicy()): Boolean {
    if (name.isNullOrBlank()) return false
    if (NEVER_FTP_INTERFACE.containsMatchIn(name)) return false
    if (policy.denied.any { it.equals(name, ignoreCase = true) }) return false
    return WIFI_INTERFACE.matches(name) || policy.tethered.any { it.equals(name, ignoreCase = true) }
}

private val WIFI_INTERFACE = Regex(
    "^(?:(?:wlan|swlan|ap|softap)\\d+|ap_br_[a-z]+\\d+|p2p\\d+|p2p-[a-z0-9]+-\\d+)$",
    RegexOption.IGNORE_CASE,
)

/**
 * Never served, matched as a name prefix: mobile data on Qualcomm (`rmnet*`), MediaTek (`ccmni*`,
 * `cc2mni*`, `ccemni*`), Unisoc (`seth_lte*`, `sipa_eth*`), Huawei (`eth_x*`), Marvell (`ccinet*`),
 * CLAT (`v4-*`, `clat*`), VPN and PPP (`tun*`, `ppp*`, `ipsec*`), USB / Bluetooth tethering
 * (`rndis*`, `ncm*`, `usb*`, `bt-pan`, `bnep*`), Ethernet (`eth*`), `dummy*` and loopback.
 */
private val NEVER_FTP_INTERFACE = Regex(
    "^(?:rmnet|cc[0-9a-z]*mni|ccinet|seth|sipa|eth|v4-|clat|pdp|ppp|tun|ipsec|rndis|ncm|usb|bt-pan|bnep|dummy|lo$)",
    RegexOption.IGNORE_CASE,
)
