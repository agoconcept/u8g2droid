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


class MainActivity : AppCompatActivity() {

    companion object {
        const val LOG_TAG: String = "MainActivity"
        const val REQUEST_ENABLE_BT: Int = 1337
        const val PREFS_NAME = "u8g2droid"
    }

    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Make sure Bluetooth is available
        assertBluetoothIsAvailable()

        // Read stored settings
        val settings = getSharedPreferences(PREFS_NAME, 0)
        val deviceName = settings.getString("deviceName", null)
        val deviceMac = settings.getString("deviceMac", null)

        if (deviceName == null || deviceMac == null) {
            selectBluetoothDevice()
        } else {
            // Start Bluetooth connection, trying to reconnect to the stored device
            startBluetoothConnection(deviceName, deviceMac)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // This is executed when Bluetooth is enabled
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled successfully", Toast.LENGTH_SHORT).show()
                val deviceName = data?.extras?.getString("deviceName")
                val deviceMac = data?.extras?.getString("deviceMac")

                // Start connection
                startBluetoothConnection(deviceName, deviceMac)

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Error when trying to enable Bluetooth", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    /**
     * Make sure that Bluetooth is available
     */
    private fun assertBluetoothIsAvailable() {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "The device does not support Bluetooth, exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Start a bluetooth connection to the selected device
     */
    private fun startBluetoothConnection(deviceName: String?, deviceMac: String?) {
        // TODO: Consider adding a broadcastreceiver in case Bluetooth status changes

        // Make sure Bluetooth is enabled
        if (! mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.putExtra("deviceName", deviceName)
            enableBtIntent.putExtra("deviceMac", deviceMac)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // TODO: Start connection as client

            Toast.makeText(
                    this,
                    "TODO: Trying to setup Bluetooth connection with '" + deviceName + "' at " + deviceMac,
                    Toast.LENGTH_SHORT).show()

            // TODO
        }
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
            if (device.name.equals(deviceName))
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
            editor.commit()

            // Start connection
            startBluetoothConnection(device?.name, device?.address)
        })
        builder.show()
    }

}
