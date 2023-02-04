package com.example.bluetoothdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel: ViewModel() {
    val latestWrittenMsg = MutableLiveData<List<String>>()
    private val _latestWrittenMsg = mutableListOf<String>()

    fun setLatestMsg(msg: String) {
        _latestWrittenMsg.add(msg)
        latestWrittenMsg.value = _latestWrittenMsg.toList()
    }
}