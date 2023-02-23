package com.example.bluetoothdemo.ble

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BleViewModel: ViewModel() {
    val isScanning = MutableLiveData<Boolean>()
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

    fun setScanning(isScanning: Boolean) {
        this.isScanning.value = isScanning
    }
}