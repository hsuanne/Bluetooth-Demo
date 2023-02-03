package com.example.bluetoothdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel: ViewModel() {
    val latestMsg = MutableLiveData<MutableList<String>>()
    private val _latestMsg = mutableListOf<String>()

    fun setLatestMsg(msg: String) {
        _latestMsg.add(msg)
        latestMsg.value = _latestMsg
    }
}