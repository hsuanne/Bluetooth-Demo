package com.example.bluetoothdemo.ble

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
    private lateinit var bleDevicesAdapter: BleDevicesAdapter
    private var bluetoothService : BluetoothLeService? = null

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            Log.d("serviceConnection", "onServiceConnected")
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetoothLeService ->
                // call functions on service to check connection and connect to devices
                if (!bluetoothLeService.initialize()) {
                    Log.e("serviceConnection", "Unable to initialize Bluetooth")
                    finish()
                }
                bleViewModel.serverBleDevice.value?.address?.let { bluetoothLeService.connect(it) }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d("serviceConnection", "onServiceDisconnected")
            bluetoothService = null
        }
    }

    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Log.d("gattUpdateReceiver", "ACTION_GATT_CONNECTED")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Log.d("gattUpdateReceiver", "ACTION_GATT_DISCONNECTED")
                    unbindService(serviceConnection)
                    bleViewModel.setServerBleDevice(null)
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
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            val bleService = bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("BleService exists:", "$bleService")
        }
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

    override fun onResume() {
        super.onResume()
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothService != null) {
            val result = bleViewModel.serverBleDevice.value?.let { bluetoothService!!.connect(it.address) }
            Log.d("BLEActivity", "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("onPause", "onPause")
        unregisterReceiver(gattUpdateReceiver)
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }
}