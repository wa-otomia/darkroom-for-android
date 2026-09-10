package io.github.wa_otomia.darkroom.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Android 17 Local Network Protection. Same value as
 * [android.os.Build.VERSION_CODES.CINNAMON_BUN]. Kept as a literal so unit
 * tests do not load the Android framework.
 */
const val LOCAL_NETWORK_PERMISSION_SDK = 37

fun needsLocalNetworkPermission(sdkInt: Int): Boolean =
    sdkInt >= LOCAL_NETWORK_PERMISSION_SDK

fun hasLocalNetworkPermission(context: Context, sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
    if (!needsLocalNetworkPermission(sdkInt)) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_LOCAL_NETWORK,
    ) == PackageManager.PERMISSION_GRANTED
}
