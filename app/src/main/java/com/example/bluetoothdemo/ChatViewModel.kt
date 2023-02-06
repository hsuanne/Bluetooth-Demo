package com.example.bluetoothdemo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatViewModel: ViewModel() {
    val writtenMsgFromClient = MutableLiveData<List<String>>()
    private val _latestWrittenMsgFromClient = mutableListOf<String>()

    val readMsgFromServer = MutableLiveData<List<String>>()



    fun setLatestMsg(msg: String) {
        _latestWrittenMsgFromClient.add(msg)
        writtenMsgFromClient.value = _latestWrittenMsgFromClient.toList()
    }
}