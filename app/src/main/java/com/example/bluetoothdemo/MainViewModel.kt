package com.example.bluetoothdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val pairedDevices = MutableLiveData<List<PairedDevice>>()

    fun updatePairedDevices(pairedDevices: List<PairedDevice>) {
        this.pairedDevices.value = pairedDevices
    }
}