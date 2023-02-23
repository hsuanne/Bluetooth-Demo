package com.example.bluetoothdemo.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.R
import com.example.btlibrary.BTHelper.btActivityResultLauncher
import com.example.btlibrary.BTHelper.launchPermissions
import com.example.btlibrary.BleHelper.getBTScanPermission

class BLEActivity: AppCompatActivity() {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private val handler = Handler()
    private lateinit var bleViewModel: BleViewModel
    private val btActivityResultLauncher = this.btActivityResultLauncher()

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val bleDevicesAdapter = BleDevicesAdapter(){  }
    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    this@BLEActivity,
                    getBTScanPermission()
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val bluetoothDevice = result.device

                val bleDevice =
                    BleDevice(
                        bluetoothDevice.name?: "unknown",
                        bluetoothDevice.address
                    )
                bleViewModel.addDevice(bleDevice)
            } else {
                launchPermissions(btActivityResultLauncher)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bleViewModel = ViewModelProvider(this)[BleViewModel::class.java]


        val bleRecyclerView = findViewById<RecyclerView>(R.id.bleDeviceRecyclerView)
        bleRecyclerView.layoutManager = LinearLayoutManager(this)
        bleRecyclerView.adapter = bleDevicesAdapter

        bleViewModel.bleDevices.observe(this) {
            bleDevicesAdapter.submitList(it)
        }

        scanLeDevice()
    }

    private fun scanLeDevice() {
        if (ActivityCompat.checkSelfPermission(
                this,
                getBTScanPermission()
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }
        } else {
            launchPermissions(btActivityResultLauncher)
        }
    }
}