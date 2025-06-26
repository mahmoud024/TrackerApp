// ✅ ✅ ✅
// 🔧 تحسين الخدمة باستخدام FusedLocationProviderClient
// 🧠 فصل الكود بنمط MVVM لاحقًا
// 🔁 إعادة استخدام deviceId

package com.example.tracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.net.HttpURLConnection
import java.net.URL

class TrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "tracking_channel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "TrackingService"
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .setWaitForAccurateLocation(true)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")

                // إرسال إلى الواجهة
                val intent = Intent("LOCATION_UPDATE")
                intent.putExtra("lat", location.latitude)
                intent.putExtra("lon", location.longitude)
                sendBroadcast(intent)

                // إرسال إلى Traccar
                sendLocationToTraccar(location)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        Toast.makeText(this, "Tracking Service started", Toast.LENGTH_SHORT).show()

        startForeground(NOTIFICATION_ID, buildNotification())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        } else {
            Log.e(TAG, "Location permission not granted")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Service destroyed")
        Toast.makeText(this, "Tracking Service stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Location")
            .setContentText("Tracking is active...")
            .setSmallIcon(R.drawable.notification)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendLocationToTraccar(location: Location) {
        Thread {
            try {
                val sharedPrefs = getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                val deviceId = sharedPrefs.getString("device_unique_id", "Mahmoud159753") ?: "Mahmoud159753"

                val url = URL("http://192.168.1.21:5055/?id=$deviceId&lat=${location.latitude}&lon=${location.longitude}")
                with(url.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    Log.d(TAG, "Sent to Traccar. Response: $responseCode")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to Traccar: ${e.message}")
            }
        }.start()
    }
}
