package com.example.bluetoothconnectivity

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothconnectivity.BluetoothLeService.LocalBinder
import com.example.bluetoothconnectivity.databinding.ActivityDeviceControlBinding
import kotlinx.android.synthetic.main.activity_main.*


class DeviceControlActivity : AppCompatActivity() {
    companion object{
        private val TAG: String?= DeviceControlActivity::class.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
    }
    private lateinit var binding:ActivityDeviceControlBinding
    private var mConnected = false
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mBluetoothLeService : BluetoothLeService? = null

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as LocalBinder).getService()
            mBluetoothLeService?.initialize()
            if (!mBluetoothLeService?.initialize()!!) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService?.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                Log.e("ON GATT CONNECTED 1", "OK")
                mConnected = true
                updateConnectionState("Status : Connected")
                invalidateOptionsMenu()
                Log.e("ON GATT CONNECTED 2", "OK")
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState("Status : Disconnected")
                invalidateOptionsMenu()
                //clearUI()
                Log.e("ON GATT DISCONNECTED", "OK")
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.
                Log.e("ON GATT SERVICE DISCOVERED", "OK")
               // displayGattServices(mBluetoothLeService?.getSupportedGattServices())
                Log.e("ON DISPLAYED SERVICES", "OK")
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                Log.e("ON GATT DATA AVAILABLE", "OK")
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent:Intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        Log.e(TAG, "onCreate: device name : $mDeviceName - device address : $mDeviceAddress", )

        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
        mBluetoothLeService?.connect(mDeviceAddress)

        binding.btnDisconnect.setOnClickListener {
                mBluetoothLeService?.disconnect()
        }

    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService?.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    private fun updateConnectionState(value: String) {
        runOnUiThread {
            binding.txtStatus.text = value
        }
    }

    private fun displayData(data: String?) {
        if (data != null) {
            //mDataField.setText(data)
        }
    }



    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }


}