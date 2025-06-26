package com.example.tracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tracker.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var latText: TextView
    private lateinit var lonText: TextView
    private var pathPoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null

    private val viewModel: MainViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fine || coarse) {
            viewModel.startTracking()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: return
            val lon = intent.getDoubleExtra("lon", 0.0)
            val location = Location("from_broadcast").apply {
                latitude = lat
                longitude = lon
            }
            viewModel.updateLocation(location)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latText = findViewById(R.id.latText)
        lonText = findViewById(R.id.lonText)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        registerReceiver(locationReceiver, IntentFilter("LOCATION_UPDATE"))

        viewModel.location.observe(this) { location ->
            updateLocationUI(location.latitude, location.longitude)
        }

        viewModel.isTracking.observe(this) { isTracking ->
            updateButtonText(isTracking)
        }

        viewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.toggleServiceButton).setOnClickListener {
            if (viewModel.isTracking.value == true) {
                viewModel.stopTracking()
            } else {
                checkLocationPermissionAndStartTracking()
            }
        }

        // سجّل الجهاز عند بدء التشغيل إذا لم يكن مسجلاً
        viewModel.registerDeviceIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(locationReceiver)
    }

    private fun checkLocationPermissionAndStartTracking() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startTracking()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun updateLocationUI(lat: Double, lon: Double) {
        latText.text = "Latitude: %.5f".format(lat)
        lonText.text = "Longitude: %.5f".format(lon)

        if (::googleMap.isInitialized) {
            googleMap.clear()
            val location = LatLng(lat, lon)
            pathPoints.add(location)

            polyline?.remove()
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(pathPoints)
                    .color(0xFF0000FF.toInt()) // أزرق
                    .width(8f)
            )

            googleMap.addMarker(MarkerOptions().position(location).title("You are here"))
            val cameraPosition = CameraPosition.Builder()
                .target(location)
                .zoom(16f)
                .bearing(0f)
                .tilt(30f)
                .build()

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null)
        }
    }

    private fun updateButtonText(isTracking: Boolean) {
        val button = findViewById<Button>(R.id.toggleServiceButton)
        button.text = if (isTracking) "⏸" else "▶"
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val middleEast = LatLng(31.9, 35.2)
        val zoomLevel = 6f
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(middleEast, zoomLevel))

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
            if (!success) Log.e("MapStyle", "Style parsing failed.")
        } catch (e: Resources.NotFoundException) {
            Log.e("MapStyle", "Can't find style. Error: ", e)
        }
    }
}
