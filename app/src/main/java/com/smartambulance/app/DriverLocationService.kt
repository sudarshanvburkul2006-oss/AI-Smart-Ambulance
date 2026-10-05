package com.smartambulance.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class DriverLocationService : Service() {

    private var vehicleId = "UNKNOWN"

    private lateinit var fusedLocationClient:
            com.google.android.gms.location.FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult) {

            val location = result.lastLocation ?: return

            val latitude = location.latitude
            val longitude = location.longitude

            val speedKmh =
                location.speed.toDouble() * 3.6

            val heading =
                if (location.hasBearing()) {
                    location.bearing.toDouble()
                } else {
                    0.0
                }

            // Send the actual registered vehicle ID
            val vehicleData = VehicleLocationRequest(
                vehicle_id = vehicleId,
                latitude = latitude,
                longitude = longitude,
                speed = speedKmh,
                heading = heading
            )

            RetrofitClient.api
                .checkVehicle(vehicleData)
                .enqueue(
                    object : retrofit2.Callback<VehicleCheckResponse> {

                        override fun onResponse(
                            call: retrofit2.Call<VehicleCheckResponse>,
                            response: retrofit2.Response<VehicleCheckResponse>
                        ) {

                            val data = response.body()

                            android.util.Log.d(
                                "DRIVER_ALERT",
                                "Vehicle=$vehicleId " +
                                        "Alert=${data?.alert} " +
                                        "Distance=${data?.distance} " +
                                        "SameDirection=${data?.same_direction}"
                            )

                            if (data?.alert == true) {

                                NotificationHelper
                                    .showAmbulanceAlert(
                                        this@DriverLocationService
                                    )
                            }
                        }

                        override fun onFailure(
                            call: retrofit2.Call<VehicleCheckResponse>,
                            t: Throwable
                        ) {
                            // Backend unavailable
                        }
                    }
                )
        }
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        // Create alert notification channel
        NotificationHelper.createChannel(this)

        // Foreground service notification
        createServiceNotification()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Get vehicle ID from DriverActivity
        vehicleId =
            intent?.getStringExtra("VEHICLE_ID")
                ?.trim()
                ?.ifEmpty { "UNKNOWN" }
                ?: "UNKNOWN"

        // Start GPS only after vehicle ID is received
        startDriverLocation()

        return START_STICKY
    }

    private fun startDriverLocation() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            )
                .setMinUpdateIntervalMillis(3000L)
                .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    private fun createServiceNotification() {

        val channelId = "driver_location_service"

        val channel = NotificationChannel(
            channelId,
            "Driver Location Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)

        val notification: Notification =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setContentTitle(
                    "🚗 Vehicle Monitoring Active"
                )
                .setContentText(
                    "Vehicle $vehicleId is monitoring for emergency ambulances"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_mylocation
                )
                .setOngoing(true)
                .build()

        startForeground(
            2001,
            notification
        )
    }

    override fun onDestroy() {

        fusedLocationClient.removeLocationUpdates(
            locationCallback
        )

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}