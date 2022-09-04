package com.example.bluetoothconnectivity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothconnectivity.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var scanDeviceAdapter: ScanDevicesAdapter
    private val TAG: String?= MainActivity::class.simpleName
    private lateinit var binding:ActivityMainBinding
    private var scanning = false
    private val handler = Handler()
    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 20000

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindActivity()
        setupRecyclerViewAdapter()
    }

    private fun bindActivity() {
        binding.btnScan.setOnClickListener {
            scanDevices()
        }
    }

    private fun setupRecyclerViewAdapter() {
        scanDeviceAdapter = ScanDevicesAdapter(object :
            ScanDevicesAdapter.RecyclerViewItemOnClickListener{
            @SuppressLint("MissingPermission")
            override fun onViewClick(position: ScanResult) {
                Log.e(TAG, "onViewClick: item $position" )
                bluetoothLeScanner.stopScan(leScanCallback)
                binding.btnScan.text = "Start Scan"
                //binding.status.text = "Status : Connecting ..."
            }
        })
        with(binding.recycler) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = scanDeviceAdapter
        }
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
                    bluetoothLeScanner.startScan(leScanCallback)
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
            Log.d("ScanDeviceActivity", "leScanCallback >>")
            Log.d("ScanDeviceActivity", "onScanResult(): ${result?.device?.address} - ${result?.device?.name}")
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

}