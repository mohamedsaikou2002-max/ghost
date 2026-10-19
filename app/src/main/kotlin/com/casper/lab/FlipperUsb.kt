package com.casper.lab

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * USB Host CDC-ACM-compatible serial diagnostic bridge.
 * This does NOT unlock broadband IQ samples, radar, physiological sensing, or through-wall imaging.
 * Flipper Zero must expose a compatible USB serial interface; permissions are requested explicitly.
 */
class FlipperUsb(
    private val activity: Activity,
    private val onState: (String) -> Unit,
    private val onText: (String) -> Unit
) {
    private val manager = activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private val actionPermission = "${activity.packageName}.USB_PERMISSION"
    private var connection: UsbDeviceConnection? = null
    private var dataInterface: UsbInterface? = null
    private var controlInterface: UsbInterface? = null
    private var input: UsbEndpoint? = null
    private var output: UsbEndpoint? = null
    private var attachedDeviceId: Int? = null
    @Volatile private var reading = false
    var state: String = "DISCONNECTED"
        private set
    var lastReceived: String = "No USB messages received"
        private set

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= 33) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    }
                    if (device?.deviceId == attachedDeviceId) disconnect("USB DEVICE DETACHED")
                }
            }
        }
    }

    init {
        current = this
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") activity.registerReceiver(receiver, filter)
    }

    fun available(): List<UsbDevice> = manager.deviceList.values.sortedBy { it.deviceName }

    fun connect(device: UsbDevice) {
        if (connection != null) disconnect()
        if (manager.hasPermission(device)) { open(device); return }
        update("WAITING FOR USB PERMISSION")
        val request = Intent(actionPermission).setClass(activity, UsbPermissionReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            activity, 0, request, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        manager.requestPermission(device, pending)
    }

    /** Delivered by our explicit, non-exported manifest receiver. */
    internal fun permissionResult(intent: Intent) {
        val device = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        }
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
            open(device)
        } else update("USB PERMISSION DENIED")
    }

    private fun open(device: UsbDevice) {
        var selected: UsbInterface? = null
        var epIn: UsbEndpoint? = null
        var epOut: UsbEndpoint? = null
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            val a = (0 until iface.endpointCount).map { iface.getEndpoint(it) }
            val bIn = a.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_IN }
            val bOut = a.firstOrNull { it.type == UsbConstants.USB_ENDPOINT_XFER_BULK && it.direction == UsbConstants.USB_DIR_OUT }
            if (bIn != null && bOut != null) {
                selected = iface; epIn = bIn; epOut = bOut; break
            }
        }
        if (selected == null || epIn == null || epOut == null) {
            update("UNSUPPORTED USB INTERFACE (NO BULK SERIAL ENDPOINTS)")
            return
        }
        val conn = manager.openDevice(device)
        if (conn == null) { update("COULD NOT OPEN USB DEVICE"); return }
        val control = (0 until device.interfaceCount).map { device.getInterface(it) }
            .firstOrNull { it.interfaceClass == UsbConstants.USB_CLASS_COMM }

        if (control != null && control.id != selected.id) conn.claimInterface(control, true)
        if (!conn.claimInterface(selected, true)) {
            conn.close(); update("USB INTERFACE BUSY"); return
        }
        if (control != null) {
            val lineCoding = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(115200).put(0).put(0).put(8).array()
            // CDC-ACM SET_LINE_CODING / SET_CONTROL_LINE_STATE. Some vendor protocols ignore these.
            conn.controlTransfer(0x21, 0x20, 0, control.id, lineCoding, 7, 500)
            conn.controlTransfer(0x21, 0x22, 3, control.id, null, 0, 500)
        }
        connection = conn
        dataInterface = selected
        controlInterface = control
        input = epIn
        output = epOut
        attachedDeviceId = device.deviceId
        reading = true
        update("USB CONNECTED · ${device.vendorId.toString(16).uppercase()}:${device.productId.toString(16).uppercase()}")
        Thread({ readLoop(conn, epIn) }, "casper-usb-reader").apply { isDaemon = true }.start()
    }

    private fun readLoop(conn: UsbDeviceConnection, endpoint: UsbEndpoint) {
        val bytes = ByteArray(2048)
        val line = StringBuilder()
        while (reading && connection === conn) {
            val size = try { conn.bulkTransfer(endpoint, bytes, bytes.size, 350) } catch (_: Exception) { -1 }
            if (size <= 0) continue
            for (i in 0 until size) {
                val c = (bytes[i].toInt() and 0xff).toChar()
                when {
                    c == '\n' || c == '\r' -> {
                        if (line.isNotEmpty()) {
                            val message = line.toString().take(600)
                            line.clear()
                            lastReceived = message
                            activity.runOnUiThread { onText(message) }
                        }
                    }
                    c.code in 32..126 || c == '\t' -> if (line.length < 600) line.append(c)
                }
            }
        }
    }

    fun sendDiagnostic() {
        val conn = connection ?: run { update("USB NOT CONNECTED"); return }
        val ep = output ?: return
        Thread({
            // Documented Flipper CLI diagnostic; no transmission/replay or radio control.
            val data = "help\r\n".toByteArray(Charsets.US_ASCII)
            val sent = try { conn.bulkTransfer(ep, data, data.size, 1500) } catch (_: Exception) { -1 }
            activity.runOnUiThread { onText(if (sent > 0) "Sent CLI help request ($sent bytes)" else "CLI write failed") }
        }, "casper-usb-write").start()
    }

    fun disconnect(message: String = "DISCONNECTED") {
        reading = false
        val old = connection
        connection = null
        try { dataInterface?.let { old?.releaseInterface(it) } } catch (_: Exception) { }
        try { controlInterface?.let { old?.releaseInterface(it) } } catch (_: Exception) { }
        try { old?.close() } catch (_: Exception) { }
        input = null; output = null; dataInterface = null; controlInterface = null; attachedDeviceId = null
        update(message)
    }

    fun close() {
        if (current === this) current = null
        disconnect()
        try { activity.unregisterReceiver(receiver) } catch (_: Exception) { }
    }

    private fun update(message: String) {
        state = message
        activity.runOnUiThread { onState(message) }
    }

    companion object {
        private var current: FlipperUsb? = null
        internal fun dispatchPermission(intent: Intent) { current?.permissionResult(intent) }
    }
}
