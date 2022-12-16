package com.example.bluetoothdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PairedDevicesAdapter: RecyclerView.Adapter<PairedDevicesAdapter.ViewHolder>() {
    private var pairedDevices: List<PairedDevice> = listOf()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.deviceName)
        private val deviceMacAddress: TextView = view.findViewById(R.id.deviceMacAddress)

        fun bind(device: PairedDevice) {
            deviceName.text = device.deviceName
            deviceMacAddress.text = device.deviceMacAddress
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paired_devices, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pairedDevices[position])
    }

    override fun getItemCount(): Int {
        return pairedDevices.size
    }

    fun updatePairedDevices(pairedDevices: List<PairedDevice>) {
        this.pairedDevices = pairedDevices
    }
}