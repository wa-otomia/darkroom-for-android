package io.github.wa_otomia.darkroom.data.ftp

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import io.github.wa_otomia.darkroom.core.FtpInterfacePolicy

/**
 * Reads [FtpInterfacePolicy] from the system each time it is asked, so the FTP server follows the
 * hotspot being switched on or off and mobile data coming and going.
 */
object FtpInterfaces {
    /** Sticky broadcast the tethering service keeps up to date; readable without a permission. */
    private const val ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED"
    private const val EXTRA_ACTIVE_TETHER = "tetherArray"
    private const val EXTRA_ACTIVE_LOCAL_ONLY = "android.net.extra.ACTIVE_LOCAL_ONLY"
    /** The same list under its Android 10 key (minSdk 29), before it moved to the namespaced one. */
    private const val EXTRA_ACTIVE_LOCAL_ONLY_Q = "localOnlyArray"

    fun policy(context: Context?): FtpInterfacePolicy {
        if (context == null) return FtpInterfacePolicy()
        return FtpInterfacePolicy(tethered = tethered(context), denied = mobileOrVpn(context))
    }

    /**
     * Interfaces the phone is currently sharing on: its hotspot (and a local-only hotspot), plus
     * USB or Bluetooth tethering, which [io.github.wa_otomia.darkroom.core.isAllowedFtpInterface]
     * still refuses by name. Empty when the broadcast cannot be read.
     */
    private fun tethered(context: Context): Set<String> = try {
        val sticky = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(ACTION_TETHER_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        buildSet {
            sticky?.getStringArrayListExtra(EXTRA_ACTIVE_TETHER)?.let(::addAll)
            sticky?.getStringArrayListExtra(EXTRA_ACTIVE_LOCAL_ONLY)?.let(::addAll)
            sticky?.getStringArrayListExtra(EXTRA_ACTIVE_LOCAL_ONLY_Q)?.let(::addAll)
        }
    } catch (_: Exception) {
        emptySet()
    }

    /** Interface names of every network the system marks as mobile data or VPN. */
    @Suppress("DEPRECATION")
    private fun mobileOrVpn(context: Context): Set<String> = try {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        buildSet {
            for (network in cm?.allNetworks.orEmpty()) {
                val caps = cm?.getNetworkCapabilities(network) ?: continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                ) {
                    cm.getLinkProperties(network)?.interfaceName?.let(::add)
                }
            }
        }
    } catch (_: Exception) {
        emptySet()
    }
}
