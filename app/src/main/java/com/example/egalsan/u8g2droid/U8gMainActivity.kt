package com.example.egalsan.u8g2droid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.widget.Toast
import android.util.Log
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.util.*


class U8gMainActivity : AppCompatActivity() {

    companion object {
        const val LOG_TAG: String = "U8gMainActivity"
        const val REQUEST_ENABLE_BT: Int = 1337
        const val PREFS_NAME = "u8g2droid"
        //TODO: const val MY_UUID = "093380fb-fc88-4b0c-930c-d1c10947ec1e"
        //TODO: Maybe use 00001101-0000-1000-8000-00805F9B34FB (from createRfcommSocketToServiceRecord() documentation)
        const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }

    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val mUuid: UUID = UUID.fromString(MY_UUID)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make sure Bluetooth is available
        if (! assertBluetoothIsAvailable()) {
            Toast.makeText(this, "The device does not support Bluetooth, exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Make sure Bluetooth is enabled
        if (! mBluetoothAdapter!!.isEnabled) {
            Log.i(LOG_TAG, "Bluetooth is not enabled, trying to enable it")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            Log.i(LOG_TAG, "Bluetooth is enabled, trying to connect to a device")
            startBT()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // This is executed when Bluetooth is enabled
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.v(LOG_TAG, "Bluetooth enabled successfully")
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
        // Read stored settings
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val deviceName = settings.getString("deviceName", null)
        val deviceMac = settings.getString("deviceMac", null)

        if (deviceName == null || deviceMac == null) {
            Log.v(LOG_TAG, "No previous device found, selecting a Bluetooth device to connect to")
            selectBluetoothDevice()
        } else {
            // Start Bluetooth connection, trying to reconnect to the stored device
            Log.v(LOG_TAG, "Previous device found, trying to reconnect to Bluetooth device")
            startBluetoothConnection(deviceName, deviceMac)
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

        // Start connection as client
        val bondedDevices = mBluetoothAdapter!!.bondedDevices
        val btDevice = getBluetoothDevice(deviceName, bondedDevices)
        val btThread = U8gConnectThread(btDevice!!)
        btThread.start()

        Log.i(LOG_TAG, "Trying to setup Bluetooth connection with '" + deviceName + "' at " + deviceMac)
        Toast.makeText(
                this,
                "Trying to setup Bluetooth connection with '" + deviceName + "' at " + deviceMac,
                Toast.LENGTH_SHORT).show()
    }


    private fun getPairedBluetoothDeviceNames(bondedDevices: Set<BluetoothDevice>): ArrayList<CharSequence> {

        val devicesNames = ArrayList<CharSequence>()

        for (device in bondedDevices) {
            Log.d(LOG_TAG, "deviceName: " + device.name)
            Log.d(LOG_TAG, "deviceMAC: " + device.address)
            devicesNames.add(device.name)
        }

        return devicesNames
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
        val devicesNames = getPairedBluetoothDeviceNames(bondedDevices)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select a device to connect to")
        builder.setItems( devicesNames.toTypedArray(), DialogInterface.OnClickListener { dialog, which ->

            // Find the BluetoothDevice
            val deviceName = devicesNames[which].toString()
            val device = getBluetoothDevice(deviceName, bondedDevices)

            Toast.makeText(this, "Selected: " + deviceName, Toast.LENGTH_SHORT).show()

            val settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val editor = settings.edit()
            editor.putString("deviceName", device?.name)
            editor.putString("deviceMac", device?.address)
            editor.apply()

            Log.i(LOG_TAG, "Stored Bluetooth device '" + device?.name + "' at " + device?.address)

            // Start connection
            startBluetoothConnection(device?.name, device?.address)
        })
        builder.show()
    }

    private inner class U8gConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?

        init {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            var tmp: BluetoothSocket? = null

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createRfcommSocketToServiceRecord(mUuid)
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Socket's create() method failed", e)
            }

            mmSocket = tmp
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter!!.cancelDiscovery()

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.e(LOG_TAG, "Unable to connect with Bluetooth device '" + mmDevice.name + "' at " + mmDevice.address)
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.e(LOG_TAG, "Could not close the client socket", closeException)
                }

                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // TODO: manageMyConnectedSocket(mmSocket)
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                Log.v(LOG_TAG, "Closing connection with Bluetooth device '" + mmDevice.name + "' at " + mmDevice.address)
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Could not close the client socket", e)
            }

        }
    }
}
