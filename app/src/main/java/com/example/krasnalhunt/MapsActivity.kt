package com.example.krasnalhunt

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.Observer
import com.example.krasnalhunt.model.AppDatabase
import com.example.krasnalhunt.model.Player
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition


const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, InitializationFragment.OnDoneListener {

    override fun onDone() {
        runOnUiThread {
            getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).edit {
                putBoolean(PREF_FIRST_LAUNCH, false)
            }

            loadMap()
        }
    }

    private lateinit var mMap: GoogleMap
    private var mLocationPermissionGranted = false
    private var mLastKnownLocation: Location? = null
    private var mFusedLocationProviderClient : FusedLocationProviderClient? = null
    var player = Player(Location("Player location"))

    private fun launchInitialization() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, InitializationFragment())
            .commit()
    }

    private fun loadMap() {
        getLocationPermission()

        val mapFragment = SupportMapFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, mapFragment, "map")
            .commit()
        mapFragment.getMapAsync(this)
    }

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        database = AppDatabase.createInstance(applicationContext)

        val firstLaunch = getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(PREF_FIRST_LAUNCH, true)

        if (firstLaunch)
            launchInitialization()
        else
            loadMap()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE).edit {
                    putBoolean(PREF_FIRST_LAUNCH, true)
                }
                AsyncTask.execute {
                    AppDatabase.instance?.clearAllTables()
                    runOnUiThread {
                        finish()
                    }
                }
                true
            }
            R.id.action_list -> {
                if (supportFragmentManager.findFragmentByTag("list") == null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content, DwarfItemListFragment.newInstance(), "list")
                        .addToBackStack(null)
                        .commit()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        database.dwarfItemDao().findItems().observe(this, Observer { dwarfs ->
            Log.d("TAG", dwarfs.toString())
            for (dwarf in dwarfs) {
                mMap.addMarker(MarkerOptions().position(dwarf.coordinates).title(dwarf.name))
            }
            val pos = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(LatLng(51.109286, 17.032307), 16.0f))
            mMap.moveCamera(pos)
        })
        if (mLocationPermissionGranted) {
            getDeviceLocation()
            mMap.isMyLocationEnabled=true
        }
    }

    private fun getLocationPermission() {
        /*
        * Request location permission, so that we can get the location of the
        * device. The result of the permission request is handled by a callback,
        * onRequestPermissionsResult.
        */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForPermissions()
        } else {
            mLocationPermissionGranted = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                } else
                    showPermissionDialog({
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            finish()
                        } else
                            askForPermissions()
                    })
            }
        }
        updateLocationUI()
    }

    private fun updateLocationUI() {
        try {
            if (mLocationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mLastKnownLocation = null
                //getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    private fun showPermissionDialog(handler: () -> Unit, message: String? = null) {
        AlertDialog.Builder(this).setTitle(R.string.location_permission_alert_title)
            .setMessage(
                when {
                    message != null -> message
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> getString(R.string.location_permission_alert_message)
                    else -> getString(R.string.location_permission_alert_message_on_dont_show_again)
                }
            )
            .setPositiveButton(getString(R.string.positive_button_text)) { _, _ -> handler() }
            .setCancelable(false)
            .create().show()
    }

    private fun askForPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
        )
    }

    companion object {
        const val PREF_FIRST_LAUNCH = "first-launch"
        const val SHARED_PREFERENCES = "shared-preferences"
    }
    private fun getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        try
        {
            if (mLocationPermissionGranted)
            {
                val location = mFusedLocationProviderClient!!.lastLocation
                location.addOnCompleteListener {task ->
                        if (task.isSuccessful)
                        {
                            val currentLocation = task.getResult() as Location
                            player.setPlayerLocation(currentLocation)
                        }
                        else
                        {

                        }
                }
            }
        }
        catch (e:SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }




}
