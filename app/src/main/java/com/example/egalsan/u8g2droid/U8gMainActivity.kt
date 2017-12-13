package com.example.egalsan.u8g2droid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.widget.Toast
import android.util.Log
import android.support.v7.app.AlertDialog
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.support.design.widget.BottomNavigationView
import kotlinx.android.synthetic.main.activity_u8g_main.*
import java.io.IOException
import java.util.*
import android.os.Looper
import android.widget.FrameLayout
import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import kotlin.collections.HashMap


class U8gMainActivity : AppCompatActivity(), U8gBluetoothCallbacks, U8gDataFeeder {

    companion object {
        const val LOG_TAG: String = "U8gMainActivity"
        const val REQUEST_ENABLE_BT: Int = 1337
        const val PREFS_NAME: String = "u8g2droid"
        const val MY_UUID: String = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val mUuid: UUID = UUID.fromString(MY_UUID)
    private var mBtThread: U8gConnectThread? = null

    var mData: String = ""


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                // TODO: message.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                // TODO: message.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                // TODO: message.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_u8g_main)

        // Get the GL container and put the U8gGLSurfaceView on it
        val frameLayout: FrameLayout? = findViewById(R.id.glDataView)
        val glSurfaceView = U8gGLSurfaceView(this, this)
        frameLayout!!.addView(glSurfaceView)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        // Start Bluetooth
        startBT()
    }


    override fun onDestroy() {
        super.onDestroy()

        mBtThread!!.cancel()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // This is executed when Bluetooth is enabled
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.i(LOG_TAG, "Bluetooth enabled successfully")
                Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show()
                startBT()
            } else if (resultCode == RESULT_CANCELED) {
                Log.e(LOG_TAG, "Error when trying to enable Bluetooth")
                Toast.makeText(this, "Error when trying to enable Bluetooth", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    /**
     * Handle the Bluetooth initialization
     */
    private fun startBT() {
        // Make sure Bluetooth is available
        if (! assertBluetoothIsAvailable()) {
            Log.e(LOG_TAG, "The device does not support Bluetooth, exiting...")
            Toast.makeText(this, "The device does not support Bluetooth, exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Make sure Bluetooth is enabled
        if (! mBluetoothAdapter!!.isEnabled) {
            Log.d(LOG_TAG, "Bluetooth is not enabled, trying to enable it")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            Log.d(LOG_TAG, "Bluetooth is enabled, trying to connect to a device")

            // Read stored settings
            val settings = getSharedPreferences(PREFS_NAME, 0)
            val deviceName = settings.getString("deviceName", null)
            val deviceMac = settings.getString("deviceMac", null)

            if (deviceName == null || deviceMac == null) {
                Log.d(LOG_TAG, "No previous device found, selecting a Bluetooth device to connect to")
                selectBluetoothDevice()
            } else {
                // Start Bluetooth connection, trying to reconnect to the stored device
                Log.d(LOG_TAG, "Previous device found, trying to reconnect to Bluetooth device")
                startBluetoothConnection(deviceName, deviceMac)
            }
        }
    }


    /**
     * Make sure that Bluetooth is available
     */
    private fun assertBluetoothIsAvailable(): Boolean {
        return mBluetoothAdapter != null
    }


    /**
     * Start a bluetooth connection to the selected device
     */
    private fun startBluetoothConnection(deviceName: String?, deviceMac: String?) {
        // TODO: Consider adding a broadcastreceiver in case Bluetooth status changes

        Log.i(LOG_TAG, "Trying to setup Bluetooth connection with '$deviceName' at $deviceMac")
        Toast.makeText(
                this,
                "Trying to setup Bluetooth connection with '$deviceName' at $deviceMac",
                Toast.LENGTH_SHORT).show()

        // Start connection as client
        val bondedDevices = mBluetoothAdapter!!.bondedDevices
        val btDevice = getBluetoothDevice(deviceName, bondedDevices)
        mBtThread = U8gConnectThread(btDevice!!, this)
        mBtThread!!.start()
    }


    /**
     * Returns a Hashmap with the device (including its MAC) as keys and the description of the device name as values
     */
    private fun getPairedBluetoothDevices(bondedDevices: Set<BluetoothDevice>): HashMap<String, String> {

        val devices = HashMap<String, String>()

        for (device in bondedDevices) {
            Log.d(LOG_TAG, "deviceName: " + device.name)
            Log.d(LOG_TAG, "deviceMAC: " + device.address)
            devices.put("${device.name} (${device.address})", device.name)
        }

        return devices
    }

    /**
     * Find a BluetoothDevice from its name
     */
    private fun getBluetoothDevice(deviceName: String?, bondedDevices: Set<BluetoothDevice>): BluetoothDevice? {

        val iter = bondedDevices.iterator()
        while (iter.hasNext()) {
            val device = iter.next()
            if (device.name == deviceName)
                return device
        }
        return null
    }

    /**
     * Select a Bluetooth device from a list and store the selection
     */
    private fun selectBluetoothDevice() {

        // Show a list of paired devices to select
        val bondedDevices = mBluetoothAdapter!!.bondedDevices
        val devicesInfo = getPairedBluetoothDevices(bondedDevices)
        val devicesNames = devicesInfo.keys.toList().sorted()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a device to connect to")
        builder.setCancelable(false)
        builder.setNegativeButton("Cancel", { dialog, _ -> dialog.cancel()})
        builder.setOnCancelListener({
            Log.e(LOG_TAG, "A device must be selected")
            Toast.makeText(this, "A device must be selected", Toast.LENGTH_SHORT).show()
            finish()
        })
        builder.setItems( devicesNames.toTypedArray(), { _, which ->

            // Find the BluetoothDevice
            val deviceName = devicesNames[which]
            val device = getBluetoothDevice(devicesInfo[deviceName], bondedDevices)

            Log.d(LOG_TAG, "Selected: $deviceName")

            // Store info about the device
            storeDeviceInfo(device?.name, device?.address)

            // Start connection
            startBluetoothConnection(device?.name, device?.address)
        })
        builder.show()
    }


    /**
     * Store deviceName and deviceMac
     */
    private fun storeDeviceInfo(deviceName: String?, deviceMac: String?) {
        val settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = settings.edit()
        editor.putString("deviceName", deviceName)
        editor.putString("deviceMac", deviceMac)
        editor.apply()

        Log.d(LOG_TAG, "Stored Bluetooth device '$deviceName' at $deviceMac")
    }


    override fun connectionSuccess() {
        Log.i(LOG_TAG, "Successfully connected to Bluetooth device")
        Toast.makeText(this, "Successfully connected to Bluetooth device", Toast.LENGTH_SHORT).show()
    }

    override fun connectionError() {
        // Clean the information about the stored device, so next time it will
        // ask for the device to connect to again
        storeDeviceInfo(null, null)

        Log.e(LOG_TAG, "Unable to connect to Bluetooth device")
        Toast.makeText(this, "Unable to connect to Bluetooth device", Toast.LENGTH_SHORT).show()

        // Retry to start Bluetooth again
        startBT()
    }

    override fun readData(data: String?) {
        Log.d(LOG_TAG, data)

        // Store the data
        mData = data.orEmpty()
    }

    override fun writeData(data: String?) {
        // TODO: Not implemented
    }

    override fun feedData(): String {
        return when (mData.length == 0) {
            true    -> "-"
            false   -> mData
        }
    }

    private inner class U8gConnectThread(private val device: BluetoothDevice, private val btCallback: U8gBluetoothCallbacks) : Thread() {
        private val socket: BluetoothSocket?
        private val inStream: InputStream?
        private val outStream: OutputStream?
        private val bufferedReader: BufferedReader?

        private val handler = Handler(Looper.getMainLooper())

        init {
            // Use temporary objects that are later assigned variables are final
            var tmpSocket: BluetoothSocket? = null
            var tmpInStream: InputStream? = null
            var tmpOutStream: OutputStream? = null

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmpSocket = device.createRfcommSocketToServiceRecord(mUuid)

                tmpInStream = tmpSocket.inputStream
                tmpOutStream = tmpSocket.outputStream
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Bluetooth socket IO failed", e)
            }

            socket = tmpSocket
            inStream = tmpInStream
            outStream = tmpOutStream
            bufferedReader = inStream!!.bufferedReader()
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter!!.cancelDiscovery()

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket!!.connect()
            } catch (connectException: IOException) {
                Log.e(LOG_TAG, "Could not connect the client socket", connectException)
                // Unable to connect; close the socket and return.
                try {
                    // Run on the UI thead
                    handler.post { btCallback.connectionError() }

                    socket!!.close()

                } catch (closeException: IOException) {
                    Log.e(LOG_TAG, "Could not close the client socket", closeException)
                }

                return
            }

            // Run on the UI thread
            handler.post { btCallback.connectionSuccess() }

            // Start the read loop
            readLoop()
        }

        private fun readLoop() {
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream
                    val str = bufferedReader!!.readLine()

                    // Send message to the UI thread
                    handler.post { btCallback.readData(str) }
                } catch (e: IOException) {
                    Log.d(LOG_TAG, "Input stream was disconnected", e)
                    break
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                Log.v(LOG_TAG, "Closing connection with Bluetooth device '" + device.name + "' at " + device.address)
                socket!!.close()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Could not close the client socket", e)
            }

        }
    }
}
