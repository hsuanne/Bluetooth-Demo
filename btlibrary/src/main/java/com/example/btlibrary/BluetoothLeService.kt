package com.example.btlibrary

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.btlibrary.SampleGattAttributes.CHARACTERISTIC_A
import com.example.btlibrary.SampleGattAttributes.DESCRIPTOR_NOTIFICATION
import com.example.btlibrary.SampleGattAttributes.MY_SERVICE
import java.util.*

class BluetoothLeService : Service() {
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectionState = STATE_DISCONNECTED
    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                if (ActivityCompat.checkSelfPermission(
                        this@BluetoothLeService,
                        BTHelper.getBTConnectPermission()
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                Log.i(TAG, "Attempting to start service discovery:" + bluetoothGatt!!.discoverServices())

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
                gatt?.close()
                Log.i(TAG, "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.i(TAG, "discovered services")

                for (service in gatt!!.services){
                    Log.i(TAG, "services: ${service.uuid}, ${service.characteristics.size}")

                    if (service.uuid.toString() == MY_SERVICE) {
                        Log.i(TAG, "my service: ${service.uuid}, ${service.characteristics.size}")

                        for (mCharacteristic in service.characteristics) {
                            Log.i(TAG, "characteristic uuid: ${mCharacteristic.uuid}")
                            if (ActivityCompat.checkSelfPermission(
                                    this@BluetoothLeService,
                                    BTHelper.getBTConnectPermission()
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return
                            }
                            if (mCharacteristic.uuid.toString() == CHARACTERISTIC_A) {
                                Log.i(TAG, "my characteristic: ${mCharacteristic.uuid}")

                                // call onCharacteristicRead()
                                BleHelper.bluetoothService?.readCharacteristic(mCharacteristic)
                            }
                        }
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead")
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Log.i(TAG, "onCharacteristicChanged")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    fun initialize(): Boolean {
        bluetoothAdapter = (applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                if (ActivityCompat.checkSelfPermission(
                        this,
                        BTHelper.getBTConnectPermission()
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@let
                }
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                Log.i(TAG, "connecting GATT")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return false
        }
        return true
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    BTHelper.getBTConnectPermission()
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return
        }
    }

    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic) {
        Log.i(TAG, "setCharacteristicNotification")
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    BTHelper.getBTConnectPermission()
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // skip setCharacteristicNotification if it was set already
            if (characteristic.getDescriptor(UUID.fromString(DESCRIPTOR_NOTIFICATION)).value.contentEquals(
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )) return

            gatt.setCharacteristicNotification(characteristic, true)

            // check for specific characteristic uuid
            if (CHARACTERISTIC_A == characteristic.uuid.toString()) {
                val descriptor = characteristic.getDescriptor(UUID.fromString(DESCRIPTOR_NOTIFICATION))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
                Log.i(TAG, "descriptor written")
            }
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "unbind")
        close()
        return super.onUnbind(intent)
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic?= null) {
        val intent = Intent(action)

        // parse hex to string
        if (characteristic != null) {
            val data: ByteArray? = characteristic.value
            Log.d(TAG, "transferring data")

            // set notification if want to keep listening to server characteristic change
            this@BluetoothLeService.setCharacteristicNotification(characteristic)

            if (data?.isNotEmpty() == true) {
                val hexString: String = data.joinToString(separator = " ") {
                    String.format("%02X", it)
                }
                val text = String(data, 0, characteristic.value.size)
                intent.putExtra(MY_DATA,
                    "my characteristic UUID: ${characteristic.uuid}\nHex: $hexString\nText: $text")
            }
        }
        sendBroadcast(intent)
    }

    private fun close() {
        bluetoothGatt?.let { gatt ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    BTHelper.getBTConnectPermission()
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            gatt.close()
            bluetoothGatt = null
        }
    }

    companion object {
        private val TAG = BluetoothLeService::class.java.name
        const val ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2

        const val MY_DATA = "my data"
    }
}