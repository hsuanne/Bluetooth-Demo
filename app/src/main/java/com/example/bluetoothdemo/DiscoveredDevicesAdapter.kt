package com.example.bluetoothdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class DiscoveredDevicesAdapter: ListAdapter<FoundDevice, RecyclerView.ViewHolder>(DiffCallback()) {

    class DiscoveredDeviceInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.deviceName)
        private val deviceMacAddress: TextView = view.findViewById(R.id.deviceMacAddress)

        fun bind(device: FoundDevice) {
            deviceName.text = device.deviceName
            deviceMacAddress.text = device.deviceMacAddress
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paired_devices, parent, false)
        return DiscoveredDeviceInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DiscoveredDeviceInfoViewHolder -> holder.bind(getItem(position))
        }
    }
}