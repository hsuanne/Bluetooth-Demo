package com.example.bluetoothdemo

import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    var isServer: Boolean = false
    val isClientConnecting = MutableLiveData<Boolean>()
    val pairedDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()
    val connectedServer = MutableLiveData<String>()
    val connectedClient = MutableLiveData<String>()
    val readMsgFromServer = MutableLiveData<List<String>>()
    private val _readMsgFromServer = mutableListOf<String>()
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

    fun setConnectedServer(deviceName: String) {
        connectedServer.postValue(deviceName)
    }

    fun setConnectedClient(deviceName: String) {
        connectedClient.postValue(deviceName)
    }

    fun setMyBTService(bluetoothService: MyBluetoothService) {
        myBluetoothService = bluetoothService
    }

    fun setMyBTSocket(bluetoothSocket: BluetoothSocket) {
        myBluetoothSocket = bluetoothSocket
    }

    fun addReadMsgFromServer(msg: String) {
        _readMsgFromServer.add(msg)
        readMsgFromServer.value = _readMsgFromServer.toList()
    }

    fun clearReadMsgFromServer() {
        _readMsgFromServer.clear()
        readMsgFromServer.value = _readMsgFromServer.toList()
    }

    fun writeMsg(message: String) {
        val msg = message.toByteArray()
        myBluetoothService.ConnectedThread(myBluetoothSocket).write(msg)
    }

    fun setIsClientConnecting(isConnecting: Boolean) {
        isClientConnecting.postValue(isConnecting)
    }
}