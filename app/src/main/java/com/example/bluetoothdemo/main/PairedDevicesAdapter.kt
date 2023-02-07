package com.example.bluetoothdemo.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.FoundDevice
import com.example.bluetoothdemo.FoundDeviceDiffCallback
import com.example.bluetoothdemo.R

class PairedDevicesAdapter(
    val onDeviceClick: (device: FoundDevice) -> Unit
): ListAdapter<FoundDevice, RecyclerView.ViewHolder>(FoundDeviceDiffCallback()) {

    class PairedDeviceInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.deviceName)
        private val deviceMacAddress: TextView = view.findViewById(R.id.deviceMacAddress)

        fun bind(device: FoundDevice) {
            deviceName.text = device.deviceName
            deviceMacAddress.text = device.deviceMacAddress
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paired_devices, parent, false)
        return PairedDeviceInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PairedDeviceInfoViewHolder -> {
                val device = getItem(position)
                holder.bind(device)
                holder.itemView.setOnClickListener {
                    onDeviceClick(device)
                }
            }
        }
    }
}