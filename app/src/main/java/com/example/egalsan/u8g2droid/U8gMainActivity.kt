package com.example.egalsan.u8g2droid

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import android.util.Log
import android.support.v7.app.AlertDialog
import android.support.design.widget.BottomNavigationView
import kotlinx.android.synthetic.main.activity_u8g_main.*
import android.widget.FrameLayout
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager


class U8gMainActivity : AppCompatActivity(), U8gDataFeeder {

    companion object {
        private const val LOG_TAG: String = "U8gMainActivity"
        private const val REQUEST_ENABLE_BT: Int = 1337
        private const val PREFS_NAME: String = "u8g2droid"
    }

    private var mBluetoothManager = U8gBluetoothManager()

    private var mData: String = ""


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

        // Instantiate a new receiver and register it
        val receiver = BluetoothReceiver()

        // Register the broadcast receiver
        var statusIntentFilter = IntentFilter(U8gBluetoothService.READ_DATA_MSG)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, statusIntentFilter)

        // Register the broadcast receiver
        statusIntentFilter = IntentFilter(U8gBluetoothService.CONNECTION_ERROR_MSG)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, statusIntentFilter)

        // Register the broadcast receiver
        statusIntentFilter = IntentFilter(U8gBluetoothService.CONNECTION_SUCCESS_MSG)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, statusIntentFilter)

        // Get the GL container and put the U8gGLSurfaceView on it
        val frameLayout: FrameLayout? = findViewById(R.id.glDataView)
        val glSurfaceView = U8gGLSurfaceView(this, this)
        frameLayout!!.addView(glSurfaceView)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        // Only start the BT service if it's not already running
        if (!isServiceRunning(U8gBluetoothService::class.java)) {
            startBT()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // Only stop the service when the activity is really finishing
        if (! isChangingConfigurations) {
            val btService = Intent(this, U8gBluetoothService::class.java)
            stopService(btService)
        }
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
        if (! mBluetoothManager.isBluetoothAvailable()) {
            Log.e(LOG_TAG, "The device does not support Bluetooth, exiting...")
            Toast.makeText(this, "The device does not support Bluetooth, exiting...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Make sure Bluetooth is enabled
        if (mBluetoothManager.isAdapterEnabled() == false) {
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
        val btDevice = mBluetoothManager.getDeviceByName(deviceName)

        // Start background service to
        val btService = Intent(this, U8gBluetoothService::class.java)
        btService.putExtra(U8gBluetoothService.DEVICE_ADDRESS_STR, btDevice?.address)
        startService(btService)
    }


    /**
     * Select a Bluetooth device from a list and store the selection
     */
    private fun selectBluetoothDevice() {

        // Show a list of paired devices to select
        val devices = mBluetoothManager.getDevices()

        // Create formatted list
        val devicesNames: ArrayList<String> = ArrayList()
        for (device in devices) {
            devicesNames.add("${device[1]} (${device[0]})")
        }

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
            val device = mBluetoothManager.getDeviceByAddress(devices[which][0])

            Log.d(LOG_TAG, "Selected: ${devices[which][1]} (${devices[which][0]})")

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

    /**
     * Check if a service is running
     */
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.getRunningServices(Integer.MAX_VALUE).forEach { service ->
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    inner class BluetoothReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(LOG_TAG, "intent: $intent")

            when (intent?.action) {

                U8gBluetoothService.CONNECTION_SUCCESS_MSG -> {
                    Log.i(LOG_TAG, "Successfully connected to Bluetooth device")
                    Toast.makeText(this@U8gMainActivity, "Successfully connected to Bluetooth device", Toast.LENGTH_SHORT).show()
                }

                U8gBluetoothService.CONNECTION_ERROR_MSG -> {
                    // Clean the information about the stored device, so next time it will
                    // ask for the device to connect to again
                    storeDeviceInfo(null, null)

                    Log.e(LOG_TAG, "Unable to connect to Bluetooth device")
                    Toast.makeText(this@U8gMainActivity, "Unable to connect to Bluetooth device", Toast.LENGTH_SHORT).show()

                    // Retry to start Bluetooth again
                    startBT()
                }

                U8gBluetoothService.READ_DATA_MSG -> {
                    val data = intent?.getStringExtra(U8gBluetoothService.DATA_STR)
                    mData = data.orEmpty()
                }
            }
        }
    }

    override fun feedData(): String {
        return when (mData.length == 0) {
            true    -> "-"
            false   -> mData
        }
    }
}
