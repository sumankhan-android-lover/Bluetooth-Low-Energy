package com.example.bluetoothconnectivity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothconnectivity.BluetoothLeService.LocalBinder
import com.example.bluetoothconnectivity.DeviceControlActivity.Companion.EXTRAS_DEVICE_ADDRESS
import com.example.bluetoothconnectivity.DeviceControlActivity.Companion.EXTRAS_DEVICE_NAME
import com.example.bluetoothconnectivity.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private var macAddress: String?=""
    private val TAG: String?= MainActivity::class.simpleName
    private lateinit var binding:ActivityMainBinding
    private lateinit var scanDeviceAdapter: ScanDevicesAdapter
    private var scanning = false
    private var connected = false
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val handler = Handler()

    private var bluetoothService : BluetoothLeService? = null
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 20000

    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothAdapter:BluetoothAdapter?=null
    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter?.bluetoothLeScanner!!
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindActivity()
        setupRecyclerViewAdapter()
    }

    private fun bindActivity() {
//        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        binding.btnScan.setOnClickListener {
            scanDevices()
        }
        binding.btnDisconnect.setOnClickListener {

        }
    }

    private fun setupRecyclerViewAdapter() {
        scanDeviceAdapter = ScanDevicesAdapter(object :
            ScanDevicesAdapter.RecyclerViewItemOnClickListener{
            @SuppressLint("MissingPermission")
            override fun onViewClick(result: ScanResult) {
                Log.e(TAG, "onViewClick: item $result" )
                bluetoothLeScanner.stopScan(leScanCallback)
                binding.btnScan.text = "Start Scan"
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Connect Bluetooth")
                    .setMessage("Do you want to connect bluetooth?")
                    .setPositiveButton("Connect") { dialog, _ ->
                        onButtonConnectClick(result)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        //updateStatus(false,"Cancel..")
                        dialog.dismiss()
                    }
                    .create()
                    .show()
                //binding.status.text = "Status : Connecting ..."
            }
        })
        with(binding.recycler) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = scanDeviceAdapter
        }
    }

    @SuppressLint("MissingPermission")
    private fun onButtonConnectClick(result: ScanResult) {
        Log.e(TAG, "onButtonConnectClick: ${result.device.uuids}" )
        macAddress = result.device.address

        if (result.device == null) return
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(EXTRAS_DEVICE_NAME, result.device.name)
        intent.putExtra(EXTRAS_DEVICE_ADDRESS, result.device.address)
        if (scanning) {
            bluetoothLeScanner.stopScan(leScanCallback)
            scanning = false
        }
        startActivity(intent)

//        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
//        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        //bluetooth.connect(address = macAddress )
        //bluetoothGatt = result.device.connectGatt(this, false, bluetoothGattCallback)
    }


    @SuppressLint("MissingPermission")
    private fun scanDevices() {
        if (!scanning) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
                binding.btnScan.text = "Start Scan"
            }, SCAN_PERIOD)
            scanning = true
            //PERMISSION COARSE LOCATION
            Log.d("ScanDeviceStart", "startScan()")
            when (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                PackageManager.PERMISSION_GRANTED -> {
                    bluetoothLeScanner.startScan(leScanCallback)//null,scanSettings,
                    binding.btnScan.text = "Stop Scan"
                }
                else -> requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            }
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            binding.btnScan.text = "Start Scan"
        }
    }

    // Device scan callback.
    private val leScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            //TODO TEST TEST TEST
            scanDeviceAdapter.addSingleItem(result)
            Log.e("ScanDeviceActivity", "leScanCallback >> UUID : ${result.scanRecord?.serviceUuids}")
            Log.d("ScanDeviceActivity", "onScanResult(): ${result.device?.address} - ${result.device?.name}")
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let { scanDeviceAdapter.setItems(it) }
            Log.d("DeviceListActivity","onBatchScanResults:${results.toString()}")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("DeviceListActivity", "onScanFailed: $errorCode")
        }
    }

/*    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address
            Log.e(TAG, "onConnectionStateChange: status change - BluetoothGatt : $gatt - status : $status - newState : $newState" )
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // successfully connected to the GATT Server
                    Log.e(TAG, "onConnectionStateChange: connected...." )
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server
                    Log.e(TAG, "onConnectionStateChange: disconnected...." )
                }
            }else{
                Log.e("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt?.close()
            }

        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

        }

    }*/


    // Code to manage Service lifecycle.
//    private val serviceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//            componentName: ComponentName,
//            service: IBinder
//        ) {
//            bluetoothService = (service as LocalBinder).service
//            bluetoothService?.let { bluetooth ->
//                if (!bluetooth.initialize()) {
//                    Log.e(TAG, "Unable to initialize Bluetooth")
//                    finish()
//                }
//                // perform device connection
//                bluetooth.connect(address = macAddress )
//            }
//        }
//
//        override fun onServiceDisconnected(componentName: ComponentName) {
//            bluetoothService = null
//        }
//    }
//
//    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            when (intent.action) {
//                BluetoothLeService.ACTION_GATT_CONNECTED -> {
//                    connected = true
//                    binding.status.text = "Connected.."
//                   // updateConnectionState(R.string.connected)
//                }
//                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
//                    connected = false
//                    binding.status.text = "Disconnected.."
//                    //updateConnectionState(R.string.disconnected)
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
//        if (bluetoothService != null) {
//            val result = bluetoothService?.connect(macAddress)
//            Log.e(TAG, "Connect request result=$result")
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        unregisterReceiver(gattUpdateReceiver)
//    }
//
//    private fun makeGattUpdateIntentFilter(): IntentFilter {
//        return IntentFilter().apply {
//            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
//            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
//        }
//    }
}