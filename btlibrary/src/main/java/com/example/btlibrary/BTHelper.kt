package com.example.btlibrary

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.btlibrary.Constants.MY_UUID
import com.example.btlibrary.Toaster.showToast
import java.io.IOException

object BTHelper {
    /***
     * The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio)
     * and is required for any and all Bluetooth activity.
     * Please first initiate a bluetoothAdapter in your activity in order to use the functions within this class.
     * val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
     * val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
     * reference: https://developer.android.com/guide/topics/connectivity/bluetooth/setup
     ***/

    /* Main Functions */

    /*** checks if the device system supports bluetooth ***/
    fun checkBluetoothSupported(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            showToast(context, context.getString(R.string.bluetooth_not_supported))
            (context as Activity).finish()
        }
    }

    /*** checks if bluetooth is enabled; if not, enables it ***/
    fun checkBluetoothEnable(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        val mContext = context as AppCompatActivity
        val requestBluetooth =
            mContext.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    // permission granted
                    showToast(mContext, mContext.getString(R.string.bluetooth_connect_granted))
                    bluetoothAdapter?.enable()
                } else {
                    // permission denied
                    showToast(mContext, mContext.getString(R.string.bluetooth_connect_not_granted))
                }
            }

        val requestMultiplePermissions =
            mContext.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    if (it.value) {
                        // permission granted
                        showToast(mContext, mContext.getString(R.string.bluetooth_connect_granted))
                        bluetoothAdapter?.enable()
                    } else {
                        // permission denied
                        showToast(mContext, mContext.getString(R.string.bluetooth_connect_not_granted))
                    }
                }
            }

        if (ActivityCompat.checkSelfPermission(context, getBTConnectPermission()) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // for android 12 and higher
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else {
                // for android 11 and lower
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }
        }
    }

    /*** get bonded devices list ***/
    fun getPairedDevices(context: Context, bluetoothAdapter: BluetoothAdapter?,
                         activityResultLauncher: ActivityResultLauncher<Array<String>>): List<FoundDevice> {

        if (ActivityCompat.checkSelfPermission(context, getBTConnectPermission())
            == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
            val foundDevices = bluetoothAdapter?.bondedDevices
                ?.map {
                    FoundDevice(
                        it.name,
                        it.address,
                        it.createRfcommSocketToServiceRecord(MY_UUID)
                    )
                }
            return foundDevices?: emptyList()
        } else {
            launchPermissions(activityResultLauncher)
            return emptyList()
        }
    }

    /*** discover nearby bluetooth devices ***/
    fun discoverDevices(context: Context, bluetoothAdapter: BluetoothAdapter?,
                                activityResultLauncher: ActivityResultLauncher<Array<String>>) {
        if (ActivityCompat.checkSelfPermission(
                context, getBTConnectPermission()
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context, getBTLocatePermission()
            ) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            } else {
                bluetoothAdapter?.startDiscovery()
            }
        } else {
            launchPermissions(activityResultLauncher, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    /*** activityResultLauncher specific for discoverDevices ***/
    fun AppCompatActivity.discoverDevicesARL(): ActivityResultLauncher<Array<String>> {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (!permissions.containsValue(false)) {
                    showToast(this, this.getString(R.string.permissions_enabled_retry))
                    return@registerForActivityResult
                }
                permissions.entries.forEach {
                    Log.d("discoverDevicesARL","Bluetooth discoverDevices, ${it.key} = ${it.value}")
                    if (!it.value) {
                        if (it.key == "android.permission.ACCESS_FINE_LOCATION"){
                            showToast(this, this.getString(R.string.prompt_location_permission))
                        } else{
                            showToast(this, this.getString(R.string.prompt_bluetooth_permission))
                        }
                    }
                }
            }
        return requestMultiplePermissions
    }

    /*** makes the device discoverable, and enables it to serve as a host to let other devices find it ***/
    fun enableDiscoverability(context: Context, bluetoothAdapter: BluetoothAdapter?,
                                      btActivityResultLauncher: ActivityResultLauncher<Array<String>>) {
        val requestCode = 1
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30)
        }

        if (ActivityCompat.checkSelfPermission(
                context, getBTConnectPermission()
            ) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
            (context as AppCompatActivity).startActivityForResult(discoverableIntent, requestCode)
        } else {
            launchPermissions(btActivityResultLauncher)
        }
    }

    /* General Functions */

    /*** get required permission by build version ***/
    fun getBTConnectPermission(): String {
        return when(Build.VERSION.SDK_INT) {
            // for android 12 and higher
            Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2 -> Manifest.permission.BLUETOOTH_CONNECT
            // for android 10 and higher
            Build.VERSION_CODES.Q, Build.VERSION_CODES.R -> Manifest.permission.ACCESS_FINE_LOCATION
            // for android 9 and lower
            else -> Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }

    /*** get required permission by build version ***/
    private fun getBTLocatePermission(): String {
        return when(Build.VERSION.SDK_INT) {
            // for android 12 and higher
            Build.VERSION_CODES.S, Build.VERSION_CODES.S_V2 -> Manifest.permission.ACCESS_FINE_LOCATION
            // for android 10 and higher
            Build.VERSION_CODES.Q, Build.VERSION_CODES.R -> Manifest.permission.ACCESS_FINE_LOCATION
            // for android 9 and lower
            else -> Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }

    /*** launch to check for bluetooth permissions ***/
    fun launchPermissions(activityResultLauncher: ActivityResultLauncher<Array<String>>, extraString: Array<String>? = null) {
        val basicPermissions = arrayOf(getBTConnectPermission())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // for android 12 and higher
            // need to request BLUETOOTH_SCAN permission whenever there's a need to search for bluetooth devices
            var extraPermissions = basicPermissions
                .toMutableList()
                .apply {
                    add(Manifest.permission.BLUETOOTH_SCAN)
                }
                .toTypedArray()

            // if extraString is supplied, add it to extraPermissions
            if (extraString?.isNotEmpty() == true) extraPermissions = extraPermissions.plus(extraString)

            activityResultLauncher.launch(extraPermissions)
            return
        } else { // for android 11 and lower
            activityResultLauncher.launch(basicPermissions)
            return
        }
    }

    /*** activityResultLauncher returns check result by showing toast;
     * activityResultLauncher must be initiated in activity to avoid runtime error ***/
    fun AppCompatActivity.btActivityResultLauncher(): ActivityResultLauncher<Array<String>> {
        val requestBluetoothPermissions =
            this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.isEmpty()) return@registerForActivityResult
                if (permissions.entries.any { !it.value }) {
                    showToast(this, getString(R.string.prompt_bluetooth_permission))
                } else {
                    showToast(this, getString(R.string.permissions_enabled_retry))
                }
            }
        return requestBluetoothPermissions
    }

    /* Classes for Bluetooth Connection */
    /*** Required if you would like to the device to act as a server(host).
     * Use start() to listen for connection requests,
     * once a client socket is found, call manageMyConnectedSocket(),
     * you might want to then close server socket.
     * note: you may need to customize this class if situation warrants
     ***/
    class AcceptThread(val context: Context, val bluetoothAdapter: BluetoothAdapter?, val manageMyConnectedSocket: (bluetoothSocket: BluetoothSocket) -> Unit) : Thread() {
        val mmServerSocket = getBluetoothSocket(bluetoothAdapter)

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    // start listening for connection requests
                    // accept() is a blocking call, so do not execute it on UI thread!
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(ContentValues.TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    // call close() immediately after finding a socket if needed
                    // mmServerSocket?.close()
                    // shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Could not close the connect socket", e)
            }
        }

        private fun getBluetoothSocket(bluetoothAdapter: BluetoothAdapter?): BluetoothServerSocket? {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    getBTConnectPermission()
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.d("getBluetoothSocket", "cannot get server socket because bluetooth permission is not enabled")
                return null
            }

            // get a bluetoothSocket
            val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
                bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(Constants.NAME, MY_UUID)
            }

            return mmServerSocket
        }
    }
}