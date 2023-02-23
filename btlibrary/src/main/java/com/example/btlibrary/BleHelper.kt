package com.example.btlibrary

import android.Manifest
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData

object BleHelper {
    const val SCAN_PERIOD: Long = 10000 // Stops scanning after 10 seconds.
    private val handler = Handler(Looper.getMainLooper())
    val isScanning = MutableLiveData<Boolean>(false)

    /* Main Functions */

    /*** scan for BLE devices
     * 1. write a callback first to get scan results
     * 2. initiate a bluetoothScanner in Activity
     * 3. initiate an ActivityResultLauncher in Activity
     * note: you may need to customize this function if situation warrants
     * ***/

    fun getBleScanCallback(context: Context, activityResultLauncher: ActivityResultLauncher<Array<String>>,
                                   manageBleDevice: (bluetoothDevice: BleDevice) -> Unit): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        getBTScanPermission()
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val bluetoothDevice = result.device
                    val bleDevice = BleDevice(
                        bluetoothDevice.name ?: "unknown",
                        bluetoothDevice.address
                    )
                    manageBleDevice(bleDevice) // can use bluetoothDevice for further action, e.g. add data to a list to show on UI
                } else {
                    BTHelper.launchPermissions(activityResultLauncher)
                }
            }
        }
    }

    fun scanBLeDevice(context: Context, bleScanner: BluetoothLeScanner,
                             bleScanCallback: ScanCallback, activityResultLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                getBTScanPermission()
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (isScanning.value == false) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    isScanning.postValue(false)
                    bleScanner.stopScan(bleScanCallback)
                }, SCAN_PERIOD)

                isScanning.postValue(true)
                bleScanner.startScan(bleScanCallback)
            } else {
                isScanning.postValue(false)
                bleScanner.stopScan(bleScanCallback)
            }
        } else {
            BTHelper.launchPermissions(activityResultLauncher, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

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