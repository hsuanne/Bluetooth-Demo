package com.example.bluetoothdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PairedDevicesAdapter: ListAdapter<PairedDevice, RecyclerView.ViewHolder>(DiffCallback()) {
    private var pairedDevices: List<PairedDevice> = listOf()

    class PairedDeviceInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.deviceName)
        private val deviceMacAddress: TextView = view.findViewById(R.id.deviceMacAddress)

        fun bind(device: PairedDevice) {
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
            is PairedDeviceInfoViewHolder -> holder.bind(getItem(position))
        }
    }

    fun updatePairedDevices(pairedDevices: List<PairedDevice>) {
        this.pairedDevices = pairedDevices
    }
}

class DiffCallback : DiffUtil.ItemCallback<PairedDevice>() {
    override fun areItemsTheSame(oldItem: PairedDevice, newItem: PairedDevice): Boolean {
        return oldItem.deviceName == newItem.deviceName
    }

    override fun areContentsTheSame(oldItem: PairedDevice, newItem: PairedDevice): Boolean {
        return oldItem == newItem
    }
}