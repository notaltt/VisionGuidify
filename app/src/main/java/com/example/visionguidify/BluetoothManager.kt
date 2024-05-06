package com.example.visionguidify

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class BluetoothManager<T>(private val activity: AppCompatActivity) {
    interface BluetoothMessageListener {
        fun onBluetoothMessageReceived(message: String)
    }

    private var messageListener: BluetoothMessageListener? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var connectedThread: ConnectedThread
    private lateinit var handler: Handler
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var device: BluetoothDevice

    init {
        initBluetooth()
    }

    private fun initBluetooth() {
        // Check Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FINE_LOCATION_PERMISSION
            )
            // Don't proceed with Bluetooth initialization here, wait for permission result
            return
        }

        // Bluetooth permissions granted, proceed with Bluetooth initialization
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, request to enable it
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // Bluetooth is enabled, proceed with connecting to the device
            connectToDevice()
        }

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MESSAGE_READ -> {
                        val readBuffer = msg.obj as ByteArray
                        val messageBT = String(readBuffer, 0, msg.arg1)
                        Log.d(TAG, "Message received from Bluetooth device: $messageBT")

                        // Notify the listener when a message is received
                        messageListener?.onBluetoothMessageReceived(messageBT)
                    }
                }
            }
        }
    }

    fun setMessageListener(listener: BluetoothMessageListener) {
        messageListener = listener
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth was enabled, proceed with connecting to the device
                    connectToDevice()
                } else {
                    // Bluetooth was not enabled, handle this scenario
                    Log.e(TAG, "Bluetooth was not enabled")
                }
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_FINE_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with connecting to the device
                    connectToDevice()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message)
                    Log.e(TAG, "Location permission denied")
                }
            }
        }
    }

    private fun connectToDevice() {
        // Check Bluetooth permissions
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FINE_LOCATION_PERMISSION
            )
            // Don't return here, allow the method to continue after permission request
        }

        // Get the BluetoothDevice with the specified MAC address
        device = bluetoothAdapter.getRemoteDevice("D8:BC:38:FB:9D:56")
        Log.d(TAG, "Device retrieved: ${device.name} - ${device.address}")

        device.let {
            // Attempt to connect to the device in a separate thread
            ConnectThread(it).start()
        }
    }

    // Thread to connect to the Bluetooth device
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {

        override fun run() {
            try {
                // Check if Bluetooth permissions are granted
                if (hasBluetoothPermissions()) {
                    Log.d(TAG, "Attempting to create Bluetooth socket...")
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                    Log.d(TAG, "Bluetooth socket created")

                    Log.d(TAG, "Attempting to connect to the socket...")
                    bluetoothSocket?.connect()
                    Log.d(TAG, "Socket connected")

                    // Start the thread to manage the connection and perform data transfer
                    connectedThread = ConnectedThread(bluetoothSocket!!)
                    connectedThread.start()
                    Log.d(TAG, "ConnectedThread started")
                } else {
                    Log.e(TAG, "Bluetooth permissions not granted")
                }
            } catch (e: IOException) {
                try {
                    bluetoothSocket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException)
                }
                Log.e(TAG, "IOException occurred: ${e.message}")
            } catch (se: SecurityException) {
                Log.e(TAG, "SecurityException occurred: ${se.message}")
            }
        }
        // Check if Bluetooth permissions are granted
        private fun hasBluetoothPermissions(): Boolean {
            return ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Thread to manage the Bluetooth connection and perform data transfer
    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {

        private val inputStream: InputStream = socket.inputStream
        private val buffer = ByteArray(1024) // buffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    numBytes = inputStream.read(buffer)
                    // Send the obtained bytes to the UI activity for display
                    handler.obtainMessage(MESSAGE_READ, numBytes, -1, buffer)
                        .sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "Input stream was disconnected", e)
                    break
                }
            }
        }

    }

    companion object {
        private const val TAG = "BluetoothManager"
        private const val REQUEST_ENABLE_BT = 1
        const val MESSAGE_READ = 0
        private const val REQUEST_FINE_LOCATION_PERMISSION = 1001
    }
}