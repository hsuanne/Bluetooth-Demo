package com.example.bluetoothdemo

import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val pairedDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()
    val connectedServer = MutableLiveData<FoundDevice>()
    private lateinit var myBluetoothService: MyBluetoothService
    private lateinit var myBluetoothSocket: BluetoothSocket

    fun updatePairedDevices(foundDevices: List<FoundDevice>) {
        this.pairedDevices.postValue(foundDevices)
    }

    fun isDevicePaired(macAddress: String): Boolean {
        return pairedDevices.value?.map { it.deviceMacAddress }?.contains(macAddress) == true
    }

    fun addToDiscoveredDevices(discoveredDevice: FoundDevice) {
        discoveredDevices.value = discoveredDevices.value?.plus(discoveredDevice)?: listOf()
    }

    fun clearDiscoveredDevices() {
        discoveredDevices.value = listOf()
    }

    fun removeDeviceAfterPaired(device: FoundDevice): Boolean {
        return if (discoveredDevices.value?.contains(device) == true) {
            discoveredDevices.postValue(discoveredDevices.value?.minus(device))
            true
        } else false
    }

    fun setConnectedServer(device: FoundDevice) {
        connectedServer.value = device
    }

    fun setMyBTService(bluetoothService: MyBluetoothService) {
        myBluetoothService = bluetoothService
    }

    fun setMyBTSocket(bluetoothSocket: BluetoothSocket) {
        myBluetoothSocket = bluetoothSocket
    }

    fun writeMsg(message: String) {
        val msg = message.toByteArray()
        myBluetoothService.ConnectedThread(myBluetoothSocket).write(msg)
    }
}