package com.example.bluetoothdemo.ble

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BleViewModel: ViewModel() {
    val bleDevices = MutableLiveData<List<BluetoothDevice>>()
    private val _bleDevices = mutableListOf<BluetoothDevice>()

    fun addDevice(bleDevice: BluetoothDevice) {
        val deviceAddress = _bleDevices.map { it.address }
        if (deviceAddress.contains(bleDevice.address)) return
        _bleDevices.add(bleDevice)
        bleDevices.value = _bleDevices.toList()
    }

    fun clearBleDevices() {
        _bleDevices.clear()
        bleDevices.value = _bleDevices.toList()
    }
}