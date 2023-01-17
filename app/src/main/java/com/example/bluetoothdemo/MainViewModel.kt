package com.example.bluetoothdemo

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val foundDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()
    val bluetoothDevices = MutableLiveData<List<BluetoothDevice>>()

    fun updatePairedDevices(foundDevices: List<FoundDevice>) {
        this.foundDevices.value = foundDevices
    }

    fun addToDiscoveredDevices(discoveredDevice: FoundDevice) {
        discoveredDevices.value = discoveredDevices.value?.plus(discoveredDevice)?: listOf()
    }

    fun addToBluetoothDevices(bluetoothDevice: BluetoothDevice) {
        bluetoothDevices.value = bluetoothDevices.value?.plus(bluetoothDevice)?: listOf()
    }

    fun clearDiscoveredDevices() {
        discoveredDevices.value = listOf()
    }

    fun removeDeviceAfterPaired(device: FoundDevice) {
        discoveredDevices.postValue(discoveredDevices.value?.minus(device))
    }
}