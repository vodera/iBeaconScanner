package com.acuvera.demoapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.polidea.rxandroidble2.RxBleClient
import androidx.core.app.ActivityCompat.startActivityForResult
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.MacAddress
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.acuvera.demoapp.adapters.DevicesAdapter
import com.acuvera.demoapp.model.Device
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.disposables.DisposableHelper.dispose
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    val compositeDisposable = CompositeDisposable()
    lateinit var rxBleClient: RxBleClient
    private val LOCATION_PERMISSION_REQUEST = 200
    lateinit var scanSubscription: Disposable
    private val devices = ArrayList<String>()
    private val rxDevices = ArrayList<Device>()
    lateinit var adapter: DevicesAdapter
    private var switchMenu = true;

    private var latitude: Double? = null
    private var longitude: Double? = null
    private var num: Double? = null
    var currentCountry: String? = null

    private val fusedLocation: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    fun getCountry(): String {
        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
        return "${addresses[0].countryName}"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rxBleClient = RxBleClient.create(this@MainActivity)
        devices.add("Any")
        adapter =  DevicesAdapter(rxDevices, this@MainActivity)//ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_list_item_1, devices)
        list_devices.adapter = adapter

        // application will request for location permission immediately it is loaded
        // this will call the location permission function
        requestLocationPermission()
    }

    // application will request for location updates once it is loaded
    override fun onResume() {
        super.onResume()
        requestLocationUpdates()
    }


    // this here is the
    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // show reason asynchronously
            resources.getString(R.string.location_permission_2).showPermissionDialog(this@MainActivity, {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST) }, { finish()})
        } else {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.nav, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_fav -> {
            // do stuff
            if(switchMenu) {
                if(currentCountry != null) {
                    item.setTitle("STOP")
                    Toast.makeText(this, "Scanning BLE", Toast.LENGTH_SHORT).show()
                    startBluetooth()
                    switchMenu = false
                } else {
                    Toast.makeText(this@MainActivity, "Wait as we scan current country", Toast.LENGTH_LONG).show()
                }
            }else {
                stopDevice()
                switchMenu = true
                item.setTitle("SCAN")
            }

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun startBluetooth() {
        Log.e("StartBlueTooth", "called")
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.e("BlueToothAdapter", "is null")
        } else {
            if (mBluetoothAdapter.isEnabled) {
                // Bluetooth is not enable :)
                scanDevices()
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val REQUEST_ENABLE_BT = 1
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
           when(requestCode) {
                1 -> scanDevices()
               200 -> startBluetooth()
           }
        }
    }

    private fun scanDevices() {
        scanSubscription = rxBleClient.scanBleDevices(ScanSettings.Builder()
                 .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // change if needed
                 .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) // change if needed
                .build())
            .subscribe(
                {
                    Log.e("BlueDevice", "macaddress = ${it.bleDevice.macAddress}")
                    addDevice(it)
                },
                { throwable ->
                    // Handle an error here.
                    Log.e("Error", " = ${throwable.message}")
                }
            )

        compositeDisposable.add(scanSubscription)
    }

    private fun addDevice(scanResult: ScanResult) {
        val isAdded = devices.contains(scanResult.bleDevice.macAddress)
        Log.e("Added", " = $isAdded")
        if(!isAdded) {
            devices.add(scanResult.bleDevice.macAddress)
            val device = Device(scanResult, currentCountry!!, latitude!!, longitude!!, currentCountry!!)
            rxDevices.add(device)
            adapter.notifyDataSetChanged()
            Log.e("Lis size", "= ${devices.size}")
        }
    }

    fun stopDevice(){
        scanSubscription.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    fun String.showPermissionDialog(context: Context, positiveHandler: () -> Unit, negativeHandler: () -> Unit, title: String ="Message",
                                    cancellable: Boolean = false, positiveText: String = "ALLOW", negativeText: String = "CANCEL") {
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(this)
            .setPositiveButton(positiveText) { dialog, _ ->
                positiveHandler()
                dialog.dismiss()
            }.setNegativeButton(negativeText ) { dialog, _ ->
                dialog.dismiss()
                negativeHandler()
            }
            .create()
        if(cancellable) dialog.setCancelable(cancellable) else dialog.setCancelable(cancellable)
        dialog.show()
    }

    fun connectDevice(position: Int) {
        Toast.makeText(this@MainActivity, " device $position", Toast.LENGTH_LONG).show()
    }

    private fun isLocationAllowed() = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates() {
        if(isLocationAllowed()) {
            // locationManager.requestLocationUpdates(provider, 0, 0f, locationListener)
            fusedLocation.lastLocation.addOnSuccessListener {
                if(it != null) {
                    Log.e("Location", "lat = ${it.latitude} long=${it.longitude}")
                    latitude = it.latitude
                    longitude = it.longitude
                    num = 2.0
                    currentCountry = getCountry()
                    Log.e("Current Country", "= $currentCountry")
                } else {
                    // TODO request gps opening
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestLocationUpdates()
                } else {
                    // show reason for request
                    resources.getString(R.string.location_permission).showPermissionDialog(this@MainActivity, { this.requestLocationPermission() }, { this@MainActivity.finish()})
                }
            }
        }
    }
}
