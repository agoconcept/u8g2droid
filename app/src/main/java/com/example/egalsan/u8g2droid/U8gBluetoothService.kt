package com.example.egalsan.u8g2droid

import android.app.IntentService
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class U8gBluetoothService: IntentService("U8gBluetoothService") {

    companion object {
        private const val LOG_TAG: String = "U8gBluetoothManager"

        const val MY_UUID: String = "00001101-0000-1000-8000-00805F9B34FB"

        const val DEVICE_ADDRESS_STR: String = "DEVICE_ADDRESS"
        const val CONNECTION_ERROR_MSG: String = "CONNECTION_ERROR"
        const val CONNECTION_SUCCESS_MSG: String = "CONNECTION_SUCCESS"
        const val READ_DATA_MSG: String = "READ_DATA"
        const val DATA_STR: String = "DATA"
    }

    private val mUuid: UUID = UUID.fromString(MY_UUID)
    private var mBluetoothManager = U8gBluetoothManager()

    private var mDevice: BluetoothDevice? = null

    private var mSocket: BluetoothSocket? = null
    private var mInStream: InputStream? = null
    private var mOutStream: OutputStream? = null
    private var mBufferedReader: BufferedReader? = null


    override fun onHandleIntent(intent: Intent?) {

        // Get device
        val deviceAddress = intent?.getStringExtra(DEVICE_ADDRESS_STR)

        mDevice = mBluetoothManager.getDeviceByAddress(deviceAddress)

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            mSocket = mDevice?.createRfcommSocketToServiceRecord(mUuid)

            mInStream = mSocket?.inputStream
            mOutStream = mSocket?.outputStream
            mBufferedReader = mInStream?.bufferedReader()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Bluetooth socket IO failed", e)
        }

        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothManager.cancelDiscovery()

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mSocket?.connect()
        } catch (connectException: IOException) {
            Log.e(LOG_TAG, "Could not connect the client socket", connectException)
            // Unable to connect; close the socket and return.
            try {
                // Send broadcast message about connection error
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(CONNECTION_ERROR_MSG))

                mSocket?.close()

            } catch (closeException: IOException) {
                Log.e(LOG_TAG, "Could not close the client socket", closeException)
            }

            return
        }

        // Send broadcast message about connection success
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(CONNECTION_SUCCESS_MSG))

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                // Read from the InputStream
                val str = mBufferedReader?.readLine()

                Log.d(LOG_TAG, "String read: ${str}")

                // Send broadcast message about connection error
                val localIntent = Intent(READ_DATA_MSG)
                localIntent.putExtra(DATA_STR, str)
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent)

            } catch (e: IOException) {
                Log.d(LOG_TAG, "Input stream was disconnected", e)
                break
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Log.v(LOG_TAG, "Closing connection with Bluetooth device '" + mDevice?.name + "' at " + mDevice?.address)
            mSocket?.close()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Could not close the client socket", e)
        }
    }
}
