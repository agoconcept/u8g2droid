package com.example.egalsan.u8g2droid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList


class U8gBluetoothManager {

    companion object {
        private const val LOG_TAG: String = "U8gBluetoothManager"
    }

    private val mBluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean = mBluetoothAdapter != null

    /**
     * Check if Bluetooth adapter is enabled
     */
    fun isAdapterEnabled(): Boolean? = mBluetoothAdapter?.isEnabled

    /**
     * Cancel discovery
     */
    fun cancelDiscovery() = mBluetoothAdapter?.cancelDiscovery()

    /**
     * Find a BluetoothDevice from its name
     */
    fun getDeviceByName(deviceName: String?): BluetoothDevice? {
        return getDeviceBy(deviceName, NAME)
    }

    /**
     * Find a BluetoothDevice from its address
     */
    fun getDeviceByAddress(deviceAddress: String?): BluetoothDevice? {
        return getDeviceBy(deviceAddress, ADDRESS)
    }

    /**
     * Generic finding function
     */
    private val NAME: Int = 0
    private val ADDRESS: Int = 1

    private fun getDeviceBy(deviceInfo: String?, type: Int): BluetoothDevice? {
        val bondedDevices = mBluetoothAdapter?.bondedDevices
        val iterator = bondedDevices?.iterator()
        if (iterator != null) {
            while (iterator.hasNext()) {
                val device = iterator.next()
                when (type) {
                    NAME -> {
                        if (device.name == deviceInfo)
                            return device
                    }
                    ADDRESS -> {
                        if (device.address == deviceInfo)
                            return device
                    }
                }
            }
        }
        return null
    }

    /**
     * Get a list of the Bluetooth devices names. Each element is a 2-tuple where the first
     * element is the device address and the second the device name
     */
    fun getDevices(): List<List<String>> {
        val bondedDevices = mBluetoothAdapter?.bondedDevices
        val devicesInfo = getPairedBluetoothDevices(bondedDevices!!)

        val devicesList = ArrayList<List<String>>()
        for (device in devicesInfo) {
            val entry = ArrayList<String>()
            entry.add(device.key)       // MAC address
            entry.add(device.value)     // Name

            devicesList.add(entry)
        }
        return devicesList
    }

    /**
     * Get a Hashmap with the device MACs as keys and the device name as values
     */
    private fun getPairedBluetoothDevices(bondedDevices: Set<BluetoothDevice>): HashMap<String, String> {

        val devices = HashMap<String, String>()

        for (device in bondedDevices) {
            Log.d(LOG_TAG, "deviceName: " + device.name)
            Log.d(LOG_TAG, "deviceMAC: " + device.address)
            devices.put(device.address, device.name)
        }

        return devices
    }

}