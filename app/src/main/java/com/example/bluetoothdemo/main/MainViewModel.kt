package com.example.bluetoothdemo.main

import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bluetoothdemo.ChatMessage
import com.example.btlibrary.FoundDevice
import com.example.btlibrary.MyBluetoothService

class MainViewModel: ViewModel() {
    var isServer: Boolean = false
    val isClientConnecting = MutableLiveData<Boolean>()
    val pairedDevices = MutableLiveData<List<FoundDevice>>()
    val discoveredDevices = MutableLiveData<List<FoundDevice>>()
    private val _discoveredDevices = mutableListOf<FoundDevice>()
    val connectedServer = MutableLiveData<String>()
    val connectedClient = MutableLiveData<String>()
    val chatMessages = MutableLiveData<List<ChatMessage>>()
    private val _chatMessages = mutableListOf<ChatMessage>()
    private lateinit var myBluetoothService: MyBluetoothService
    private lateinit var myBluetoothSocket: BluetoothSocket

    fun updatePairedDevices(foundDevices: List<FoundDevice>) {
        this.pairedDevices.postValue(foundDevices)
    }

    fun isDevicePaired(macAddress: String): Boolean {
        return pairedDevices.value?.map { it.deviceMacAddress }?.contains(macAddress) == true
    }

    fun addToDiscoveredDevices(discoveredDevice: FoundDevice) {
        val discoveredAddress = _discoveredDevices.map { it.deviceMacAddress }
        if (!discoveredAddress.contains(discoveredDevice.deviceMacAddress)) {
            _discoveredDevices.add(discoveredDevice)
            discoveredDevices.value = _discoveredDevices.toList()
        }
    }

    fun clearDiscoveredDevices() {
        _discoveredDevices.clear()
        discoveredDevices.value = _discoveredDevices
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

    fun addChatMessage(chatMessage: ChatMessage) {
        _chatMessages.add(chatMessage)
        chatMessages.value = _chatMessages.toList()
    }

    fun clearChatMessages() {
        _chatMessages.clear()
        chatMessages.value = _chatMessages.toList()
    }

    fun writeMsg(message: String) {
        addChatMessage(ChatMessage(message, true))
        val msg = message.toByteArray()
        myBluetoothService.ConnectedThread(myBluetoothSocket).write(msg)
    }

    fun setIsClientConnecting(isConnecting: Boolean) {
        isClientConnecting.postValue(isConnecting)
    }

    fun startTransferData() {
        myBluetoothService.ConnectedThread(myBluetoothSocket).start()
    }
}