package com.casper.lab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Explicit receiver for Android 14+ compliant mutable USB permission PendingIntent. */
class UsbPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        FlipperUsb.dispatchPermission(intent)
    }
}
