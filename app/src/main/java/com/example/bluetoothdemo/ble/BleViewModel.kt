package com.example.bluetoothdemo.ble

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BleViewModel: ViewModel() {
    val bleDevices = MutableLiveData<List<BleDevice>>()
    private val _bleDevices = mutableListOf<BleDevice>()

    fun addDevice(bleDevice: BleDevice) {
        _bleDevices.add(bleDevice)
        bleDevices.value = _bleDevices
    }
}