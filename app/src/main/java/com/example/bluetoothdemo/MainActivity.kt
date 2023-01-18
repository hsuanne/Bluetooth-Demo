package com.example.bluetoothdemo

import android.Manifest
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.IOException
import java.util.*
import java.util.UUID.fromString

class MainActivity : AppCompatActivity() {
    private lateinit var pairedDeviceButton: Button
    private lateinit var discoverDeviceButton: Button
    private lateinit var hostButton: Button
    private lateinit var messageTextView: TextView
    private lateinit var pairedDeviceRecyclerView: RecyclerView
    private lateinit var discoveredDeviceRecyclerView: RecyclerView
    private lateinit var pairedDevicesAdapter: PairedDevicesAdapter
    private lateinit var discoveredDevicesAdapter: DiscoveredDevicesAdapter
    private lateinit var mainViewModel: MainViewModel
    private lateinit var mainHandler: Handler
    private lateinit var myBluetoothService: MyBluetoothService
    private lateinit var mConnectThread: ConnectThread
    private val bluetoothPermission = getBTPermission()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio)
        // and is required for any and all Bluetooth activity
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        mainViewModel = MainViewModel()
        mainHandler = Handler(Looper.getMainLooper(), object : Handler.Callback {
            override fun handleMessage(message: Message): Boolean {
                when(message.what) {
                    MESSAGE_READ -> {
                        val readBuf = message.obj as ByteArray
                        val readMsg = String(readBuf)
                        messageTextView.text = readMsg
                    }
                }
                return true
            }
        })

        myBluetoothService = MyBluetoothService(mainHandler)

        pairedDeviceButton = findViewById(R.id.pairedDeviceButton)
        discoverDeviceButton = findViewById(R.id.discoverDeviceButton)
        hostButton = findViewById(R.id.hostButton)
        messageTextView = findViewById(R.id.transferredData)
        pairedDeviceRecyclerView = findViewById(R.id.pairedDeviceRecyclerView)
        discoveredDeviceRecyclerView = findViewById(R.id.discoverDeviceRecyclerView)

        pairedDevicesAdapter = PairedDevicesAdapter {
            val bluetoothSocket = mConnectThread.getConnectedSocket()?: return@PairedDevicesAdapter
            val hello = byteArrayOf(0x48, 101, 108, 108, 111)
            if (bluetoothSocket.isConnected) myBluetoothService.ConnectedThread(bluetoothSocket).write(hello)
        }

        discoveredDevicesAdapter = DiscoveredDevicesAdapter {
            mConnectThread = ConnectThread(it, bluetoothAdapter).apply { start() } // connect as client
        }

        pairedDeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        pairedDeviceRecyclerView.adapter = pairedDevicesAdapter

        discoveredDeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        discoveredDeviceRecyclerView.adapter = discoveredDevicesAdapter

        mainViewModel.pairedDevices.observe(this) {
            pairedDevicesAdapter.submitList(it)
        }

        mainViewModel.discoveredDevices.observe(this) {
            discoveredDevicesAdapter.submitList(it)
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

        // Enabling discoverability (= serve as host to let other devices find you)
        // this is only necessary when you want your app to host a server socket that accepts incoming connections
        // however, it's better to let app be able to host, so that we can ensure that app can accept incoming connections
        enableDiscoverability()

        // Connect as a server
        connectAsServer(bluetoothAdapter)
    }

    private fun getBTPermission(): ActivityResultLauncher<Array<String>> {
        val requestBluetoothPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.any { !it.value }) {
                    showToast(getString(R.string.prompt_bluetooth_permission))
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_enabled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        return requestBluetoothPermissions
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun connectAsServer(bluetoothAdapter: BluetoothAdapter?) {
        AcceptThread(bluetoothAdapter).start()
    }

    private fun enableDiscoverability() {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.entries.any { !it.value }) {
                    showToast(getString(R.string.prompt_bluetooth_permission))
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.permission_enabled), Toast.LENGTH_SHORT).show()
                }
            }

        hostButton.setOnClickListener {
            val requestCode = 1
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60)
            }
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                // for android 12 and higher
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ){
                // for android 10 and higher
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            ) {
                // for android 9 and lower
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else startActivityForResult(discoverableIntent, requestCode)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this@MainActivity,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    Log.d("MainActivity onReceive: ", "${device?.name}, ${device?.address}")

                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        // for android 12 and higher
                        bluetoothPermission.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                    } else if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ){
                        // for android 10 and higher
                        bluetoothPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    } else if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    ) {
                        // for android 9 and lower
                        bluetoothPermission.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        device?.let {
                            // if device is paired already, do not add to discoveredDevices
                            if (mainViewModel.isDevicePaired(it.address)) return@let
                            val deviceName = device.name ?: "Unknown"
                            val deviceHardwareAddress = device.address
                            val deviceSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                            val discoveredDevice =
                                FoundDevice(deviceName, deviceHardwareAddress, deviceSocket)
                            mainViewModel.addToDiscoveredDevices(discoveredDevice)
                        }
                    }
                }
            }
        }
    }

    private fun discoverDevices(bluetoothAdapter: BluetoothAdapter?) {
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("MainActivity discoverDevices","Bluetooth discoverDevices, ${it.key} = ${it.value}")
                    if (!it.value) {
                        if (it.key == "android.permission.ACCESS_FINE_LOCATION"){
                            Toast.makeText(this, "Please enable permissions for location.", Toast.LENGTH_SHORT).show()
                        } else{
                            Toast.makeText(this, "Please enable permissions for bluetooth scan.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (it.key != "android.permission.ACCESS_FINE_LOCATION" && bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
                        Toast.makeText(this, "Permissions enabled, please click 'discover devices' again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        discoverDeviceButton.setOnClickListener{
            mainViewModel.clearDiscoveredDevices()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                // for android 12 and higher
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                )
            } else if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ){
                // for android 10 and higher
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            ) {
                // for android 9 and lower
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            } else {
                bluetoothAdapter?.startDiscovery()
            }
        }
    }

    private fun checkPairedDevices(
        bluetoothAdapter: BluetoothAdapter?
    ) {
        pairedDeviceButton.setOnClickListener {
            getPairedDevices(bluetoothAdapter, bluetoothPermission)
        }
    }

    private fun getPairedDevices(
        bluetoothAdapter: BluetoothAdapter?,
        requestBTPermission: ActivityResultLauncher<Array<String>>
    ) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        ) {
            // for android 12 and higher
            requestBTPermission.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            // for android 10 and higher
            requestBTPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        ) {
            // for android 9 and lower
            requestBTPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
            bluetoothAdapter?.bondedDevices
                ?.map {
                    FoundDevice(
                        it.name, it.address, it.createRfcommSocketToServiceRecord(
                            MY_UUID
                        )
                    )
                }
                .apply {
                    if (this != null) mainViewModel.updatePairedDevices(this)
                }
        }
    }

    private fun checkBluetoothSupported(bluetoothAdapter: BluetoothAdapter?) {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            showToast(getString(R.string.bluetooth_not_supported))
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
                } else {
                    // for android 11 and lower
                    // todo: not yet tested, but should work...
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestBluetooth.launch(enableBtIntent)
                }
            } else {
                bluetoothAdapter.enable()
            }
        }
    }

    private fun getBluetoothSocket(bluetoothAdapter: BluetoothAdapter?): BluetoothServerSocket? {
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
                return null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                requestMultiplePermissions.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
        // get a bluetoothSocket
        val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID)
        }
        return mmServerSocket
    }

    private inner class AcceptThread(bluetoothAdapter: BluetoothAdapter?) : Thread() {
        val mmServerSocket = getBluetoothSocket(bluetoothAdapter)

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    // start listening for connection requests
                    // accept() is a blocking call, so do not execute it on UI thread!
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    // call close() immediately after finding a socket
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
            // todo: transfer data
            myBluetoothService.ConnectedThread(bluetoothSocket).start()
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(val foundDevice: FoundDevice, val bluetoothAdapter: BluetoothAdapter?) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            foundDevice.socket
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()
                } catch (e: IOException) {
                    e.message?.let { Log.d("ConnectThread", it) }
                    runOnUiThread { Toast.makeText(this@MainActivity, getString(R.string.pairing_failed), Toast.LENGTH_SHORT).show() }
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                if (socket.isConnected) manageMyConnectedSocket(socket)
            }
        }

        private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
            mainViewModel.removeDeviceAfterPaired(foundDevice)
            getPairedDevices(bluetoothAdapter, bluetoothPermission)
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }

        fun getConnectedSocket(): BluetoothSocket? {
            return mmSocket
        }
    }

    companion object {
        const val NAME = "BluetoothDemo"
        val MY_UUID: UUID = fromString("ca94f29c-ec41-4d46-9392-64188ab9b55e") // has to be the same on server and client
    }
}