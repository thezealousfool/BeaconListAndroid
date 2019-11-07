package com.vivekroy.navcog

import VvkAdapter
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.util.*

data class Beacon(val major: Int, val minor: Int, val rssi: Int) {
    companion object {
        val DIFF_CB = object : DiffUtil.ItemCallback<Beacon>() {
            override fun areItemsTheSame(oldItem: Beacon, newItem: Beacon): Boolean = ((oldItem.major == newItem.major) and (oldItem.minor == newItem.minor))
            override fun areContentsTheSame(oldItem: Beacon, newItem: Beacon): Boolean = (oldItem.rssi == newItem.rssi) }
    }
}

class MainActivity : AppCompatActivity() {
    val TAG = "vvk:mA:"
    var isScanning = false
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothScanner : BluetoothLeScanner? = null
    private val requestCode = 0
    private val queue = Channel<ScanResult>(Channel.UNLIMITED)
    private var adapter : ListAdapter<Beacon, VvkAdapter.VvkViewHolder>? = null
    private val mapToRender : MutableMap<String, Beacon> = mutableMapOf()
    private var cListener : Job? = null
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            enableDisableButton()
        }
    }
    private val scanCallback = object: ScanCallback(), CoroutineScope by MainScope() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null)
                launch {
                    queue.send(result)
                }
        }
    }
    private lateinit var scanSettings : ScanSettings
    private lateinit var scanFilter: ScanFilter

    private fun setScanFilter() {
        val uuid = UUID.fromString("F7826DA6-4FA2-4E98-8024-BC5B71E0893E")
        val bb = ByteBuffer.allocate(16)
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        val uuidBytes = bb.array()
        val manufacturerData = ByteBuffer.allocate(23)
        val manufacturerDataMask = ByteBuffer.allocate(23)
        manufacturerData.put(0, 2)  // 0x02
        manufacturerData.put(1, 21) // 0x15
        for (i in 2..17) {
            manufacturerData.put(i, uuidBytes[i-2])
        }
        for (i in 0..17) {
            manufacturerDataMask.put(i,1)
        }
        val scanFilterBuilder = ScanFilter.Builder()
        scanFilterBuilder.setManufacturerData(76, manufacturerData.array(), manufacturerDataMask.array())
        scanFilter = scanFilterBuilder.build()
    }

    private fun setScanSettings() {
        val scanSettingBuilder = ScanSettings.Builder()
        scanSettingBuilder.setReportDelay(0)
        scanSettingBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        scanSettings = scanSettingBuilder.build()
    }

    private fun enableDisableButton() {
         if(!bluetoothAdapter.isEnabled or
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
             bluetoothScanner = null
             fab.hide()
         } else {
             bluetoothScanner = bluetoothAdapter.bluetoothLeScanner
             fab.show()
         }
    }

    private fun initScanSettings() {
        setScanSettings()
        setScanFilter()
    }

    private fun createCardListener() {
        // Every second
        cListener = CoroutineScope(Dispatchers.Main).launch {
            while(true) {
                delay(500)
                adapter?.submitList(mapToRender.values.toList())
            }
        }
    }

    private fun initConsumer() {
        CoroutineScope(Dispatchers.Default).launch {
            while(true) {
                while(!queue.isEmpty) {
                    val result = queue.receive()
                    val manufacturerData = result.scanRecord?.getManufacturerSpecificData(76)!!
                    val majorMinor = ByteBuffer.wrap(manufacturerData, 18, 4).asShortBuffer()
                    val major = majorMinor[0].toUShort().toString()
                    val minor = majorMinor[1].toUShort().toString()
                    val id = "${major}_${minor}"
                    mapToRender[id] = Beacon(major.toInt(), minor.toInt(), result.rssi)
                }
                for (x in 1..10) {
                    delay(300)
                    if (!queue.isEmpty)
                        break
                }
                if (queue.isEmpty)
                    break
            }
        }
    }

    private fun startScanning() {
        isScanning = true
        fab.setImageResource(R.drawable.ic_pause)
        bluetoothScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        initConsumer()
        createCardListener()
    }

    private fun stopScanning() {
        bluetoothScanner?.stopScan(scanCallback)
        mapToRender.clear()
        adapter?.submitList(mutableListOf()) {
            isScanning = false
            fab.setImageResource(R.drawable.ic_play)
            cListener?.cancel()
        }
    }

    fun startClicked(view : View) {
        val constraints = ConstraintSet()
        if (!isScanning) {
            startScanning()
            constraints.clone(this, R.layout.activity_main_results)
            TransitionManager.beginDelayedTransition(root)
            constraints.applyTo(root)
        } else {
            constraints.clone(this, R.layout.activity_main)
            TransitionManager.beginDelayedTransition(root)
            constraints.applyTo(root)
            stopScanning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        initScanSettings()
        adapter = VvkAdapter() as ListAdapter<Beacon, VvkAdapter.VvkViewHolder>
        list.adapter = adapter!!
    }

    override fun onResume() {
        super.onResume()
        enableDisableButton()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) = enableDisableButton()
}
