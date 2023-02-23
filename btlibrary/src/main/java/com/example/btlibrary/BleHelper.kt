package com.example.btlibrary

import android.Manifest
import android.os.Build

object BleHelper {
    /* General Functions */

    /*** get required permission by build version ***/
    fun getBTScanPermission(): String {
        return when(Build.VERSION.SDK_INT) {
            // for android 12 and higher
            Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2 -> Manifest.permission.BLUETOOTH_SCAN
            // for android 10 and higher
            Build.VERSION_CODES.Q, Build.VERSION_CODES.R -> Manifest.permission.ACCESS_FINE_LOCATION
            // for android 9 and lower
            else -> Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }
}