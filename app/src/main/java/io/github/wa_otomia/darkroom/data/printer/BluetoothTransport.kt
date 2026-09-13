package io.github.wa_otomia.darkroom.data.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import io.github.wa_otomia.darkroom.core.SPP_UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class NearbyDevice(
    val mac: String,
    val name: String,
    val rssi: Int? = null,
    val bonded: Boolean = false,
    val printerLike: Boolean = false,
)

val PRINTER_NAME_RE = Regex("xiaomi|mi\\s|米家|printer|ricott|kdr|照片打印|打印|portable\\s*photo|pocket\\s*photo", RegexOption.IGNORE_CASE)

fun isPrinterLike(name: String): Boolean = PRINTER_NAME_RE.containsMatchIn(name)

class BluetoothBytePipe(private val socket: BluetoothSocket) : BytePipe {
    private val stream = StreamBytePipe(socket.inputStream, socket.outputStream) { socket.close() }
    override fun write(data: ByteArray) = stream.write(data)
    override fun readExact(n: Int, timeoutMs: Int): ByteArray = stream.readExact(n, timeoutMs)
    override fun close() = stream.close()
}

@Singleton
class PrinterBluetooth @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter: BluetoothAdapter?
        get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun isEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun bonded(): List<NearbyDevice> {
        val a = adapter ?: return emptyList()
        return try {
            a.bondedDevices.orEmpty().map { it.toNearby(bonded = true) }.sortedWith(nearbyComparator)
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun scan(timeoutMs: Long = 12_000): List<NearbyDevice> = withContext(Dispatchers.Main) {
        val a = adapter ?: return@withContext emptyList()
        val found = LinkedHashMap<String, NearbyDevice>()
        bonded().forEach { found[it.mac] = it }
        val done = AtomicBoolean(false)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        } ?: return
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .takeIf { it != Short.MIN_VALUE }?.toInt()
                        val item = device.toNearby(bonded = device.bondState == BluetoothDevice.BOND_BONDED, rssi = rssi)
                        found[item.mac] = item
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> done.set(true)
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            },
        )
        try {
            a.cancelDiscovery()
            a.startDiscovery()
            withTimeout(timeoutMs) {
                while (!done.get()) {
                    kotlinx.coroutines.delay(200)
                }
            }
        } catch (_: Exception) {
        } finally {
            a.cancelDiscovery()
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
        found.values.sortedWith(nearbyComparator)
    }

    @SuppressLint("MissingPermission")
    suspend fun pair(mac: String): Boolean = withContext(Dispatchers.IO) {
        val device = adapter?.getRemoteDevice(mac.uppercase()) ?: return@withContext false
        if (device.bondState == BluetoothDevice.BOND_BONDED) return@withContext true
        suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
                    val d = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (d?.address?.equals(mac, true) != true) return
                    when (d.bondState) {
                        BluetoothDevice.BOND_BONDED -> if (cont.isActive) cont.resume(true)
                        BluetoothDevice.BOND_NONE -> if (cont.isActive) cont.resume(false)
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            cont.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
            if (!device.createBond() && cont.isActive) {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
                cont.resume(false)
            }
        }.also {
            try {
                // receiver cleaned in callback
            } catch (_: Exception) {
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(mac: String): BluetoothBytePipe {
        val a = adapter ?: error("找不到蓝牙设备")
        // Needle for UserErrors ds_error_bluetooth_off. Do not reword without
        // updating that rule.
        if (!a.isEnabled) throw IOException("蓝牙未开启")
        val device = a.getRemoteDevice(mac.uppercase()) ?: error("找不到蓝牙设备")
        a.cancelDiscovery()
        val uuid = UUID.fromString(SPP_UUID)
        val socket = try {
            try {
                device.createRfcommSocketToServiceRecord(uuid)
            } catch (_: Exception) {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                m.invoke(device, 1) as BluetoothSocket
            }
        } catch (e: Exception) {
            throw bluetoothConnectError(e)
        }
        try {
            socket.connect()
        } catch (e: IOException) {
            try {
                socket.close()
            } catch (_: Exception) {
            }
            throw bluetoothConnectError(e)
        }
        return BluetoothBytePipe(socket)
    }

    // Needle for UserErrors ds_error_bluetooth_connect. Do not reword without
    // updating that rule; otherwise the UI shows a network error.
    private fun bluetoothConnectError(e: Throwable): IOException {
        val cause = if (e is InvocationTargetException) e.cause ?: e else e
        return IOException("无法连接蓝牙设备", cause)
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toNearby(bonded: Boolean, rssi: Int? = null): NearbyDevice {
        val n = name.orEmpty()
        return NearbyDevice(
            mac = address.uppercase(),
            name = n.ifBlank { address.uppercase() },
            rssi = rssi,
            bonded = bonded,
            printerLike = isPrinterLike(n),
        )
    }

    private val nearbyComparator = compareBy<NearbyDevice> { if (it.printerLike) 0 else 1 }
        .thenByDescending { it.rssi ?: -200 }
        .thenBy { it.name }
}
