package com.example.bluetoothdemo

import androidx.recyclerview.widget.DiffUtil

data class FoundDevice(
    val deviceName: String,
    val deviceMacAddress: String,
)

class DiffCallback : DiffUtil.ItemCallback<FoundDevice>() {
    override fun areItemsTheSame(oldItem: FoundDevice, newItem: FoundDevice): Boolean {
        return oldItem.deviceName == newItem.deviceName
    }

    override fun areContentsTheSame(oldItem: FoundDevice, newItem: FoundDevice): Boolean {
        return oldItem == newItem
    }
}