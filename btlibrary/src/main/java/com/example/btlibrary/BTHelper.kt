package com.example.btlibrary

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.example.btlibrary.Toaster.showToast

object BTHelper {
    fun checkBluetoothSupported(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            showToast(context, context.getString(R.string.bluetooth_not_supported))
            (context as Activity).finish()
        }
    }
}