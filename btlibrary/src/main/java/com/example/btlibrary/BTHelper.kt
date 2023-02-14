package com.example.btlibrary

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.example.btlibrary.Toaster.showToast

object BTHelper {
    /***
     * The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio)
     * and is required for any and all Bluetooth activity.
     * Please first initiate a bluetoothAdapter in your activity in order to use the functions within this class.
     * val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
     * val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
     * reference: https://developer.android.com/guide/topics/connectivity/bluetooth/setup
     */

    /* functions */

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

        if (ActivityCompat.checkSelfPermission(
                mContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
        } else {
            bluetoothAdapter?.enable()
        }
    }
}