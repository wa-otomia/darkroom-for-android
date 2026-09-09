package app.darkroom.android.core

/** Camera FTP may only accept sockets whose local interface is wlan0 or wlan1. */
fun isAllowedFtpInterface(name: String?): Boolean = name == "wlan0" || name == "wlan1"
