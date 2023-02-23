package com.example.bluetoothdemo.ble

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.btlibrary.BleDevice

class BleViewModel: ViewModel() {
    val bleDevices = MutableLiveData<List<BleDevice>>()
    private val _bleDevices = mutableListOf<BleDevice>()

    fun addDevice(bleDevice: BleDevice) {
        val deviceAddress = _bleDevices.map { it.deviceMacAddress }
        if (deviceAddress.contains(bleDevice.deviceMacAddress)) return
        _bleDevices.add(bleDevice)
        bleDevices.value = _bleDevices.toList()
    }

    fun clearBleDevices() {
        _bleDevices.clear()
        bleDevices.value = _bleDevices.toList()
    }
}