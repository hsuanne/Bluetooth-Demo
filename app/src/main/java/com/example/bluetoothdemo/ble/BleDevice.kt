package com.example.bluetoothdemo.ble

import androidx.recyclerview.widget.DiffUtil

data class BleDevice(
    val deviceName: String,
    val deviceMacAddress: String,
)

object BleDeviceDiffCallback : DiffUtil.ItemCallback<BleDevice>() {
    override fun areItemsTheSame(oldItem: BleDevice, newItem: BleDevice): Boolean {
        return oldItem.deviceName == newItem.deviceName
    }

    override fun areContentsTheSame(oldItem: BleDevice, newItem: BleDevice): Boolean {
        return oldItem == newItem
    }
}