package com.example.bluetoothdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val foundDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()

    fun updatePairedDevices(foundDevices: List<FoundDevice>) {
        this.foundDevices.value = foundDevices
    }

    fun addToDiscoveredDevices(discoveredDevice: FoundDevice) {
        discoveredDevices.value = discoveredDevices.value?.plus(discoveredDevice)?: listOf()
    }

    fun clearDiscoveredDevices() {
        discoveredDevices.value = listOf()
    }
}