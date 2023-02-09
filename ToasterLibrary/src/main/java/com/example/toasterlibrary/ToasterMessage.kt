package com.example.toasterlibrary

import android.content.Context
import android.widget.Toast


object ToasterMessage {
    fun toastMyMessage(c: Context?, message: String?) {
        Toast.makeText(c, message, Toast.LENGTH_SHORT).show()
    }
}