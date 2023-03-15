package com.example.bluetoothdemo.ble

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.content.*
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.R
import com.example.btlibrary.BTHelper.btActivityResultLauncher
import com.example.btlibrary.BleHelper
import com.example.btlibrary.BleHelper.registerUpdateReceiver
import com.example.btlibrary.BleUUID.MY_SERVICE
import com.example.btlibrary.BluetoothLeService
import com.example.btlibrary.BluetoothLeService.Companion.MY_DATA

class BLEActivity: AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bleBtn: Button
    private lateinit var bleViewModel: BleViewModel
    private val btActivityResultLauncher = this.btActivityResultLauncher()
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private lateinit var bleDataAdapter: ArrayAdapter<String>
    private lateinit var bleDataRecyclerView: ListView
    private val bleData = mutableListOf<String?>().apply {
        add("my service UUID: $MY_SERVICE")
    }

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = BleHelper.getServiceConnection { bleService ->
        // call functions on service to check connection and connect to devices
        if (!bleService.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth")
            finish()
        }
        bleViewModel.serverBleDevice.value?.address?.let { bleService.connect(it) }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Log.d(TAG, "ACTION_GATT_CONNECTED")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Log.d(TAG, "ACTION_GATT_DISCONNECTED")
                    unbindService(serviceConnection)
                    bleViewModel.setServerBleDevice(null)
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Log.d(TAG, "ACTION_GATT_SERVICES_DISCOVERED")
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    Log.d(TAG, "ACTION_DATA_AVAILABLE")
                    displayData(intent.getStringExtra(MY_DATA))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        // viewmodel
        bleViewModel = ViewModelProvider(this)[BleViewModel::class.java]

        // ui
        bleBtn = findViewById(R.id.bleBtn)

        bleDevicesAdapter = BleDevicesAdapter {
            bleViewModel.setServerBleDevice(it)
            BleHelper.bindBleService(this, serviceConnection)
        }
        val bleRecyclerView = findViewById<RecyclerView>(R.id.bleDeviceRecyclerView)
        bleRecyclerView.layoutManager = LinearLayoutManager(this)
        bleRecyclerView.adapter = bleDevicesAdapter

        bleDataRecyclerView = findViewById(R.id.bleData)

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

    private fun displayData(data: String?) {
        bleData.add(data)
        bleDataAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, bleData)
        bleDataRecyclerView.adapter = bleDataAdapter
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        registerUpdateReceiver(gattUpdateReceiver)
        bleViewModel.serverBleDevice.value?.address?.let { BleHelper.connectBleService(it) }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        unregisterReceiver(gattUpdateReceiver)
    }

    companion object {
        private val TAG = BLEActivity::class.java.name
    }
}