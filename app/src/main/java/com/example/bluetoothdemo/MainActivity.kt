package com.example.bluetoothdemo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var pairedDeviceButton: Button
    private lateinit var discoverDeviceButton: Button
    private lateinit var pairedDeviceRecyclerView: RecyclerView
    private lateinit var pairedDevicesAdapter: PairedDevicesAdapter
    private lateinit var mainViewModel: MainViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel = MainViewModel()

        pairedDeviceButton = findViewById(R.id.pairedDeviceButton)
        discoverDeviceButton = findViewById(R.id.discoverDeviceButton)
        pairedDeviceRecyclerView = findViewById(R.id.pairedDeviceRecyclerView)
        pairedDevicesAdapter = PairedDevicesAdapter()

        // The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio)
        // and is required for any and all Bluetooth activity
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        pairedDeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        pairedDeviceRecyclerView.adapter = pairedDevicesAdapter

        mainViewModel.pairedDevices.observe(this) {
            pairedDevicesAdapter.submitList(it)
        }

        // check if device supports bluetooth
        checkBluetoothSupported(bluetoothAdapter)

        // check if bluetooth is enabled
        checkBluetoothEnable(bluetoothAdapter)

        // check if desired device is already known
        checkPairedDevices(bluetoothAdapter)

        // start discovering devices
        // The process is asynchronous and returns a boolean value indicating whether discovery has successfully started.
        // The discovery process usually involves an inquiry scan of about 12 seconds
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        discoverDevices(bluetoothAdapter)


    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.any { !it.value }) {
                    Toast.makeText(this@MainActivity, "Please enable permissions for bluetooth.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Permissions enabled, please click 'paired devices' again.", Toast.LENGTH_SHORT).show()
                }
            }

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // for android 12 and higher
                            requestMultiplePermissions.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        }
                        return
                    }
                    println("qwer: ${device?.name}, ${device?.address}")
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                }
            }
        }
    }

    private fun discoverDevices(bluetoothAdapter: BluetoothAdapter?) {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    println("Bluetooth discoverDevices, ${it.key} = ${it.value}")
                    if (!it.value) {
                        if (it.key == "android.permission.ACCESS_FINE_LOCATION"){
                            Toast.makeText(this, "Please enable permissions for location.", Toast.LENGTH_SHORT).show()
                        } else{
                            Toast.makeText(this, "Please enable permissions for bluetooth scan.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        bluetoothAdapter?.enable()
                    }
                }
            }

        discoverDeviceButton.setOnClickListener{
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Please enable permissions for coarse location.", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // for android 12 and higher
                    requestMultiplePermissions.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                }
                return@setOnClickListener
            }
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            } else {
                bluetoothAdapter?.startDiscovery()
            }
        }
    }

    private fun checkPairedDevices(bluetoothAdapter: BluetoothAdapter?) {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.any { !it.value }) {
                    Toast.makeText(this, "Please enable permissions for bluetooth.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permissions enabled, please click 'paired devices' again.", Toast.LENGTH_SHORT).show()
                    bluetoothAdapter?.enable()
                }
            }

        pairedDeviceButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // for android 12 and higher
                    requestMultiplePermissions.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                }
            } else {
                if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
                bluetoothAdapter?.bondedDevices
                    ?.map {
                        PairedDevice(it.name, it.address)
                    }
                    .apply {
                        if (this != null) mainViewModel.updatePairedDevices(this)
                    }
            }
        }
    }

    private fun checkBluetoothSupported(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkBluetoothEnable(bluetoothAdapter: BluetoothAdapter?) {
        val requestBluetooth =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    //granted
                    Toast.makeText(this, "bluetooth permission granted", Toast.LENGTH_SHORT).show()
                    bluetoothAdapter?.enable()
                } else {
                    //deny
                    Toast.makeText(this, "bluetooth permission NOT granted!", Toast.LENGTH_SHORT).show()
                }
            }

        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("Bluetooth MainActivity", "${it.key} = ${it.value}")
                    if (!it.value) {
                        Toast.makeText(this, "Please enable permissions for bluetooth.", Toast.LENGTH_SHORT).show()
                    } else {
                        bluetoothAdapter?.enable()
                    }
                }
            }

        if (bluetoothAdapter?.isEnabled == false) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // for android 12 and higher
                    requestMultiplePermissions.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    )
                    return
                } else {
                    // for android 11 and lower
                    // todo: not yet tested, but should work...
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestBluetooth.launch(enableBtIntent)
                }
                return
            } else {
                bluetoothAdapter.enable()
            }
        }
    }
}