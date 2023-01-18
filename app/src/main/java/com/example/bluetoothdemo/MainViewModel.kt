package com.example.bluetoothdemo

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val pairedDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()
    val bluetoothDevices = MutableLiveData<List<BluetoothDevice>>()

    fun updatePairedDevices(foundDevices: List<FoundDevice>) {
        this.pairedDevices.postValue(foundDevices)
    }

    fun isDevicePaired(macAddress: String): Boolean {
        return pairedDevices.value?.map { it.deviceMacAddress }?.contains(macAddress) == true
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