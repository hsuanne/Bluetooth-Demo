package com.example.bluetoothdemo.ble

import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.R
import com.example.btlibrary.BTHelper

class BleDevicesAdapter(
    val onDeviceClick: (device: BluetoothDevice) -> Unit
): ListAdapter<BluetoothDevice, RecyclerView.ViewHolder>(BluetoothDeviceDeviceDiffCallback()) {

    class PairedDeviceInfoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = view.findViewById(R.id.deviceName)
        private val deviceMacAddress: TextView = view.findViewById(R.id.deviceMacAddress)

        fun bind(device: BluetoothDevice) {
            if (ActivityCompat.checkSelfPermission(
                    itemView.context,
                    BTHelper.getBTConnectPermission()
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("BleDevicesAdapter", "missing Bluetooth_Connect permission")
                return
            }
            deviceName.text = device.name?: "unknown"
            deviceMacAddress.text = device.address
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

class BluetoothDeviceDeviceDiffCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
    override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
        return oldItem.address == newItem.address
    }

    override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
        return oldItem == newItem
    }
}