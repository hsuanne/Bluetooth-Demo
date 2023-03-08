package com.example.btlibrary

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData

object BleHelper {
    private val TAG = BleHelper::class.java.name
    private const val SCAN_PERIOD: Long = 10000 // Stops scanning after 10 seconds.
    private val handler = Handler(Looper.getMainLooper())
    val isScanning = MutableLiveData(false)
    var bluetoothService : BluetoothLeService? = null

    /* Main Functions */

    /*** scan for BLE devices
     * 1. write a callback first to get scan results
     * 2. initiate a bluetoothScanner in Activity
     * 3. initiate an ActivityResultLauncher in Activity
     * note: you may need to customize this function if situation warrants
     * ***/

    fun getBleScanCallback(context: Context, activityResultLauncher: ActivityResultLauncher<Array<String>>,
                                   manageBleDevice: (bluetoothDevice: BluetoothDevice) -> Unit): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                if (ActivityCompat.checkSelfPermission(
                        context,
                        getBTScanPermission()
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val bluetoothDevice = result.device
                    manageBleDevice(bluetoothDevice) // can use bluetoothDevice for further action, e.g. add data to a list to show on UI
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

    /*** connect to a GATT server on a BLE device
     * 1. initiate serviceConnection in Activity
     * 2. call bindBleService where you want to start the connection
     * 3. initiate gattUpdateReceiver: BroadcastReceiver in Activity to listen for updates
     * 4. in onResume, call registerReceiver; in onPause, call unregisterReceiver
     * note: you may need to customize this function if situation warrants
     * ***/

    fun getServiceConnection(manageBleService: (bleService: BluetoothLeService) -> Unit): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceConnected(
                componentName: ComponentName,
                service: IBinder
            ) {
                bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
                bluetoothService?.let { bleService ->
                    // call functions on service to check connection and connect to devices
                    Log.d(TAG, "onServiceConnected")
                    manageBleService(bleService)
                }
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                Log.d(TAG, "onServiceDisconnected")
                bluetoothService = null
            }
        }
    }

    fun bindBleService(context: Context, serviceConnection: ServiceConnection) {
        val gattServiceIntent = Intent(context, BluetoothLeService::class.java)
        context.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun connectBleService(address: String) {
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(address)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    fun Context.registerUpdateReceiver(gattUpdateReceiver: BroadcastReceiver) {
        this.registerReceiver(gattUpdateReceiver, getGattUpdateIntentFilter())
    }

    private fun getGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
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

    /* Classes */
    object SampleGattAttributes {
        private val attributes: HashMap<String, String> = HashMap()
        const val MY_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb"
        const val CHARACTERISTIC_A = "0000ffe2-0000-1000-8000-00805f9b34fb"
        const val CHARACTERISTIC_B = "ea49b906-f574-4082-bb68-26a5170cfe91"
        const val DESCRIPTOR_NOTIFICATION = "00002902-0000-1000-8000-00805f9b34fb"


        init {
            // Sample Services.
            attributes[MY_SERVICE] = "My Service"
            // Sample Characteristics.
            attributes[CHARACTERISTIC_A] = "My Characteristic A"
            attributes[CHARACTERISTIC_B] = "My Characteristic B"
        }

        fun lookup(uuid: String?, defaultName: String?): String? {
            val name = attributes[uuid]
            return name ?: defaultName
        }
    }
}