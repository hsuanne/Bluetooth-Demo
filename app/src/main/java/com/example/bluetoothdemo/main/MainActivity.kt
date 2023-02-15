package com.example.bluetoothdemo.main

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
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.btlibrary.FoundDevice
import com.example.bluetoothdemo.*
import com.example.bluetoothdemo.chat.ChatFragment
import com.example.btlibrary.BTHelper
import com.example.btlibrary.BTHelper.btActivityResultLauncher
import com.example.btlibrary.Constants.MY_UUID
import com.example.btlibrary.Constants.NAME
import java.io.IOException
import java.util.*
import java.util.UUID.fromString

class MainActivity : AppCompatActivity() {
    private lateinit var pairedDeviceButton: Button
    private lateinit var discoverDeviceButton: Button
    private lateinit var hostButton: Button
    private lateinit var pairedDeviceRecyclerView: RecyclerView
    private lateinit var discoveredDeviceRecyclerView: RecyclerView
    private lateinit var connectProgress: ProgressBar
    private lateinit var pairedDevicesAdapter: PairedDevicesAdapter
    private lateinit var discoveredDevicesAdapter: DiscoveredDevicesAdapter
    private lateinit var mainViewModel: MainViewModel
    private lateinit var mainHandler: Handler
    private lateinit var myBluetoothService: MyBluetoothService
    private lateinit var mConnectThread: ConnectThread
    private val btActivityResultLauncher = this.btActivityResultLauncher()
    private var currentDeviceName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio)
        // and is required for any and all Bluetooth activity
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainHandler = Handler(Looper.getMainLooper(), object : Handler.Callback {
            override fun handleMessage(message: Message): Boolean {
                when(message.what) {
                    MESSAGE_READ -> {
                        val readBuf = message.obj as ByteArray
                        val readMsg = String(readBuf, 0, message.arg1)
                        mainViewModel.addChatMessage(ChatMessage(readMsg, false))
                    }
                }
                return true
            }
        })

        myBluetoothService = MyBluetoothService(mainHandler)

        pairedDeviceButton = findViewById(R.id.pairedDeviceButton)
        discoverDeviceButton = findViewById(R.id.discoverDeviceButton)
        hostButton = findViewById(R.id.hostButton)
        connectProgress = findViewById(R.id.connectProgress)
        pairedDeviceRecyclerView = findViewById(R.id.pairedDeviceRecyclerView)
        discoveredDeviceRecyclerView = findViewById(R.id.discoverDeviceRecyclerView)

        pairedDevicesAdapter = PairedDevicesAdapter {
            if (mainViewModel.isServer) {
                // check which device is connected to serverSocket
                if (it.deviceName == mainViewModel.connectedClient.value) {
                    mainViewModel.setConnectedClient(it.deviceName)
                    mainViewModel.setConnectedServer(currentDeviceName)
                    navToChatFrag()
                } else {
                    Toast.makeText(this, "This device is not a connected client.", Toast.LENGTH_SHORT).show()
                }
            } else { // is Client
                val pairedDeviceSocket = it.socket
                Log.d("pairedDeviceSocket", "isConnected: ${pairedDeviceSocket.isConnected}")

                if (!pairedDeviceSocket.isConnected) {
                    ConnectThread(it, bluetoothAdapter).apply { start() }
                } else {
                    navToChatFrag()
                }
            }
        }

        discoveredDevicesAdapter = DiscoveredDevicesAdapter {
            mConnectThread = ConnectThread(it, bluetoothAdapter).apply { start() } // connect as client
        }

        pairedDeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        pairedDeviceRecyclerView.adapter = pairedDevicesAdapter

        discoveredDeviceRecyclerView.layoutManager = LinearLayoutManager(this)
        discoveredDeviceRecyclerView.adapter = discoveredDevicesAdapter

        // observe MainViewModel
        with(mainViewModel) {
            pairedDevices.observe(this@MainActivity) {
                pairedDevicesAdapter.submitList(it)
            }

            discoveredDevices.observe(this@MainActivity) {
                discoveredDevicesAdapter.submitList(it)
            }

            isClientConnecting.observe(this@MainActivity) {
                if (it) connectProgress.visibility = View.VISIBLE
                else connectProgress.visibility = View.INVISIBLE
            }
        }

        // check if device supports bluetooth
        BTHelper.checkBluetoothSupported(this, bluetoothAdapter)

        // check if bluetooth is enabled
        BTHelper.checkBluetoothEnable(this, bluetoothAdapter)

        // check if desired device is already known
        checkPairedDevices(bluetoothAdapter)

        // start discovering devices
        // The process is asynchronous and returns a boolean value indicating whether discovery has successfully started.
        // The discovery process usually involves an inquiry scan of about 12 seconds
        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        }
        registerReceiver(receiver, filter)
        discoverDevices(bluetoothAdapter)

        // Enabling discoverability (= serve as host to let other devices find you)
        // this is only necessary when you want your app to host a server socket that accepts incoming connections
        // however, it's better to let app be able to host, so that we can ensure that app can accept incoming connections
        enableDiscoverability(bluetoothAdapter)

        // Connect as a server
        connectAsServer(bluetoothAdapter)
    }

    private fun checkPairedDevices(
        bluetoothAdapter: BluetoothAdapter?
    ) {
        mainViewModel.updatePairedDevices(BTHelper.getPairedDevices(this, bluetoothAdapter, btActivityResultLauncher))
        pairedDeviceButton.setOnClickListener {
            mainViewModel.updatePairedDevices(BTHelper.getPairedDevices(this, bluetoothAdapter, btActivityResultLauncher))
        }
    }

    private fun navToChatFrag() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, ChatFragment())
            .addToBackStack("Chat Fragment")
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun connectAsServer(bluetoothAdapter: BluetoothAdapter?) {
        AcceptThread(bluetoothAdapter).start()
    }

    private fun enableDiscoverability(bluetoothAdapter: BluetoothAdapter?) {
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
                btActivityResultLauncher.launch(
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
                btActivityResultLauncher.launch(
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
                btActivityResultLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                if (bluetoothAdapter?.isEnabled == false) bluetoothAdapter.enable()
                startActivityForResult(discoverableIntent, requestCode)
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    Log.d("MainActivity BroadcastReceiver onReceive ACTION_FOUND: ", "${device?.name}, ${device?.address}")

                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            BTHelper.getBTPermission()
                        ) == PackageManager.PERMISSION_GRANTED) {
                        device?.let {
                            // if device is paired already, do not add to discoveredDevices
                            if (mainViewModel.isDevicePaired(it.address)) return@let
                            val deviceName = device.name ?: "Unknown"
                            val deviceHardwareAddress = device.address
                            val deviceSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                            val discoveredDevice = FoundDevice(deviceName, deviceHardwareAddress, deviceSocket)
                            mainViewModel.addToDiscoveredDevices(discoveredDevice)
                        }
                    } else { // launch activityResultLauncher
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // for android 12 and higher
                            btActivityResultLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        } else {
                            // for android 11 and lower
                            btActivityResultLauncher.launch(
                                arrayOf(
                                    BTHelper.getBTPermission()
                                )
                            )
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d("MainActivity BroadcastReceiver onReceive ACTION_ACL_CONNECTED: ", "${device?.name}, ${device?.address}")
                    device?.name?.let { mainViewModel.setConnectedClient(it) }
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
                        if (mainViewModel.pairedDevices.value.isNullOrEmpty()) {
                            mainViewModel.updatePairedDevices(BTHelper.getPairedDevices(this, bluetoothAdapter, btActivityResultLauncher))
                        }
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

    private inner class AcceptThread(val bluetoothAdapter: BluetoothAdapter?) : Thread() {
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
//                    mmServerSocket?.close()
//                    shouldLoop = false
                }
            }
        }

        private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
            mainViewModel.isServer = true
            mainViewModel.setMyBTSocket(bluetoothSocket)
            mainViewModel.setMyBTService(myBluetoothService)
            setCurrentDeviceName()
        }

        private fun setCurrentDeviceName() {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                return
            } else if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            ) {
                return
            } else if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
            ) {
                return
            }
            currentDeviceName = bluetoothAdapter?.name.toString()
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

        private lateinit var clientDeviceName: String

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            bluetoothAdapter?.cancelDiscovery()
            clientDeviceName = bluetoothAdapter?.name.toString()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    mainViewModel.setIsClientConnecting(true)
                    socket.connect()
                    mainViewModel.setIsClientConnecting(false)
                } catch (e: IOException) {
                    mainViewModel.setIsClientConnecting(false)
                    e.message?.let { Log.d("ConnectThread", it) }
                    runOnUiThread { Toast.makeText(this@MainActivity, getString(R.string.pairing_failed), Toast.LENGTH_SHORT).show() }
                }

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                if (socket.isConnected) manageMyConnectedSocket(socket)
            }
        }

        private fun manageMyConnectedSocket(bluetoothSocket: BluetoothSocket) {
            mainViewModel.isServer = false
            val isClickFromDiscoverDevices = mainViewModel.removeDeviceAfterPaired(foundDevice)
            if (isClickFromDiscoverDevices) {
                mainViewModel.updatePairedDevices(BTHelper.getPairedDevices(this@MainActivity, bluetoothAdapter, btActivityResultLauncher))
                cancel()
            }
            else {
                mainViewModel.setConnectedClient(clientDeviceName)
                mainViewModel.setConnectedServer(foundDevice.deviceName)
                mainViewModel.setMyBTSocket(bluetoothSocket)
                mainViewModel.setMyBTService(myBluetoothService)
                navToChatFrag()
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}