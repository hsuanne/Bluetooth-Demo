package com.example.bluetoothdemo.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.R
import com.example.btlibrary.BTHelper.btActivityResultLauncher
import com.example.btlibrary.BleHelper

class BLEActivity: AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bleBtn: Button
    private lateinit var bleViewModel: BleViewModel
    private val btActivityResultLauncher = this.btActivityResultLauncher()
    private val bleDevicesAdapter = BleDevicesAdapter {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        // viewmodel
        bleViewModel = ViewModelProvider(this)[BleViewModel::class.java]

        // ui
        bleBtn = findViewById(R.id.bleBtn)

        val bleRecyclerView = findViewById<RecyclerView>(R.id.bleDeviceRecyclerView)
        bleRecyclerView.layoutManager = LinearLayoutManager(this)
        bleRecyclerView.adapter = bleDevicesAdapter

        // bluetooth
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // scan BLE devices
        val bleCallback = BleHelper.getBleScanCallback(this, btActivityResultLauncher) {
                bleViewModel.addDevice(it)
            }

        bleBtn.setOnClickListener {
            bleViewModel.clearBleDevices()
            BleHelper.scanBLeDevice(this, bluetoothLeScanner, bleCallback, btActivityResultLauncher)
        }

        // observe data
        bleViewModel.bleDevices.observe(this) {
            bleDevicesAdapter.submitList(it)
        }

        BleHelper.isScanning.observe(this) {
            setScanningUI(it)
        }
    }

    private fun setScanningUI(isScanning: Boolean) {
        when(isScanning) {
            true -> {
                bleBtn.isEnabled = false
                bleBtn.text = "Scanning..."
            }

            false -> {
                bleBtn.isEnabled = true
                bleBtn.text = "Scan"
            }
        }
    }
}