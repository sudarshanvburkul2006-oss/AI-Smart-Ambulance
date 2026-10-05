package com.smartambulance.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    // ---------------------------------------------------------
    // LOCATION
    // ---------------------------------------------------------

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ---------------------------------------------------------
    // FIREBASE
    // ---------------------------------------------------------

    private val firestore = FirebaseFirestore.getInstance()

    // Ambulance ID
    private val ambulanceId = "AMB001"

    // ---------------------------------------------------------
    // LIVE GPS STATE
    // ---------------------------------------------------------

    private var currentLatitude by mutableStateOf(0.0)
    private var currentLongitude by mutableStateOf(0.0)
    private var currentSpeed by mutableStateOf(0.0)
    private var currentHeading by mutableStateOf(0.0)

    private var gpsActive by mutableStateOf(false)

    // ---------------------------------------------------------
    // EMERGENCY STATE
    // ---------------------------------------------------------

    private var emergencyActive by mutableStateOf(false)

    // ---------------------------------------------------------
    // HOSPITAL STATE
    // ---------------------------------------------------------

    private var fastestHospital by
    mutableStateOf<FastestHospital?>(null)

    private var hospitalLoading by
    mutableStateOf(false)

    // ---------------------------------------------------------
    // ROUTE STATE
    // ---------------------------------------------------------

    private var routeCoordinates by
    mutableStateOf<List<LatLng>>(emptyList())

    private var routeLoading by
    mutableStateOf(false)

    // ---------------------------------------------------------
    // UPDATE AMBULANCE LOCATION IN FIRESTORE
    // ---------------------------------------------------------

    private fun updateAmbulanceLocationInFirestore(
        latitude: Double,
        longitude: Double
    ) {

        // IMPORTANT:
        // Explicit <String, Any> fixes the Firestore
        // MutableMap<String, Any> type mismatch.
        val data = hashMapOf<String, Any>(
            "latitude" to latitude,
            "longitude" to longitude,
            "isActive" to true,
            "isAvailable" to true
        )

        firestore
            .collection("ambulances")
            .document(ambulanceId)
            .update(data)
            .addOnSuccessListener {

                android.util.Log.d(
                    "FIRESTORE_LOCATION",
                    "Ambulance location updated: $latitude, $longitude"
                )
            }
            .addOnFailureListener { error ->

                android.util.Log.e(
                    "FIRESTORE_LOCATION",
                    "Failed to update ambulance location",
                    error
                )
            }
    }

    // ---------------------------------------------------------
    // FIND FASTEST HOSPITAL
    // ---------------------------------------------------------

    private fun findFastestHospital() {

        // Don't call backend before GPS is ready
        if (
            currentLatitude == 0.0 ||
            currentLongitude == 0.0
        ) {

            android.util.Log.w(
                "HOSPITAL_SEARCH",
                "GPS location is not ready yet"
            )

            return
        }

        hospitalLoading = true

        android.util.Log.d(
            "HOSPITAL_SEARCH",
            "Searching hospital near $currentLatitude, $currentLongitude"
        )

        RetrofitClient.api
            .getFastestHospital(
                latitude = currentLatitude,
                longitude = currentLongitude,
                radius = 10000
            )
            .enqueue(
                object : Callback<FastestHospitalResponse> {

                    override fun onResponse(
                        call: Call<FastestHospitalResponse>,
                        response: Response<FastestHospitalResponse>
                    ) {

                        hospitalLoading = false

                        android.util.Log.d(
                            "HOSPITAL_SEARCH",
                            "HTTP Code: ${response.code()}"
                        )

                        if (!response.isSuccessful) {

                            android.util.Log.e(
                                "HOSPITAL_SEARCH",
                                "Hospital API failed: ${response.code()} ${response.message()}"
                            )

                            fastestHospital = null
                            return
                        }

                        val result = response.body()

                        android.util.Log.d(
                            "HOSPITAL_SEARCH",
                            "Response: $result"
                        )

                        if (
                            result != null &&
                            result.success &&
                            result.fastest_hospital != null
                        ) {

                            fastestHospital =
                                result.fastest_hospital

                            android.util.Log.d(
                                "HOSPITAL_SEARCH",
                                "Fastest hospital: ${result.fastest_hospital.name}"
                            )

                        } else {

                            fastestHospital = null

                            android.util.Log.w(
                                "HOSPITAL_SEARCH",
                                "No fastest hospital returned"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<FastestHospitalResponse>,
                        t: Throwable
                    ) {

                        hospitalLoading = false
                        fastestHospital = null

                        android.util.Log.e(
                            "HOSPITAL_SEARCH",
                            "Hospital API connection failed",
                            t
                        )
                    }
                }
            )
    }

    // ---------------------------------------------------------
    // LOAD ROUTE TO HOSPITAL
    // ---------------------------------------------------------

    private fun loadRouteToHospital() {

        val hospital = fastestHospital

        if (hospital == null) {

            android.util.Log.w(
                "ROUTE",
                "No hospital selected"
            )

            return
        }

        if (
            currentLatitude == 0.0 ||
            currentLongitude == 0.0
        ) {

            android.util.Log.w(
                "ROUTE",
                "GPS location is not ready"
            )

            return
        }

        routeLoading = true

        routeCoordinates = emptyList()

        android.util.Log.d(
            "ROUTE",
            "Requesting route from " +
                    "$currentLatitude,$currentLongitude " +
                    "to ${hospital.latitude},${hospital.longitude}"
        )

        RetrofitClient.api
            .getRoute(
                startLat = currentLatitude,
                startLon = currentLongitude,
                endLat = hospital.latitude,
                endLon = hospital.longitude
            )
            .enqueue(
                object : Callback<RouteResponse> {

                    override fun onResponse(
                        call: Call<RouteResponse>,
                        response: Response<RouteResponse>
                    ) {

                        routeLoading = false

                        android.util.Log.d(
                            "ROUTE",
                            "HTTP Code: ${response.code()}"
                        )

                        if (!response.isSuccessful) {

                            android.util.Log.e(
                                "ROUTE",
                                "Route API failed: ${response.code()} ${response.message()}"
                            )

                            return
                        }

                        val result = response.body()

                        android.util.Log.d(
                            "ROUTE",
                            "Route response: $result"
                        )

                        if (
                            result == null ||
                            !result.success ||
                            result.geometry == null
                        ) {

                            android.util.Log.e(
                                "ROUTE",
                                "No route geometry received"
                            )

                            return
                        }

                        val coordinates =
                            result.geometry.coordinates.mapNotNull { point ->

                                if (point.size >= 2) {

                                    // GeoJSON format:
                                    // [longitude, latitude]

                                    LatLng(
                                        point[1],
                                        point[0]
                                    )

                                } else {
                                    null
                                }
                            }

                        if (coordinates.size >= 2) {

                            routeCoordinates = coordinates

                            android.util.Log.d(
                                "ROUTE",
                                "Route loaded with ${coordinates.size} points"
                            )

                        } else {

                            android.util.Log.e(
                                "ROUTE",
                                "Route contains insufficient coordinates"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<RouteResponse>,
                        t: Throwable
                    ) {

                        routeLoading = false

                        android.util.Log.e(
                            "ROUTE",
                            "Route API connection failed",
                            t
                        )
                    }
                }
            )
    }

    // ---------------------------------------------------------
    // LOCATION PERMISSION
    // ---------------------------------------------------------

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocation =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarseLocation =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true

            if (fineLocation || coarseLocation) {

                startGps()
            }
        }

    // ---------------------------------------------------------
    // NOTIFICATION PERMISSION
    // ---------------------------------------------------------

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Permission result handled automatically
        }

    // ---------------------------------------------------------
    // GPS CALLBACK
    // ---------------------------------------------------------

    private val locationCallback =
        object : LocationCallback() {

            override fun onLocationResult(
                result: LocationResult
            ) {

                val location =
                    result.lastLocation ?: return

                // ---------------------------------------------
                // LIVE GPS DATA
                // ---------------------------------------------

                val latitude =
                    location.latitude

                val longitude =
                    location.longitude

                val speedKmh =
                    location.speed.toDouble() * 3.6

                val headingValue =
                    if (location.hasBearing()) {
                        location.bearing.toDouble()
                    } else {
                        0.0
                    }

                // ---------------------------------------------
                // UPDATE UI
                // ---------------------------------------------

                currentLatitude = latitude
                currentLongitude = longitude
                currentSpeed = speedKmh
                currentHeading = headingValue

                gpsActive = true

                // ---------------------------------------------
                // UPDATE FIRESTORE
                // ---------------------------------------------

                updateAmbulanceLocationInFirestore(
                    latitude = latitude,
                    longitude = longitude
                )

                // ---------------------------------------------
                // SEND GPS TO FASTAPI
                // ---------------------------------------------

                val ambulanceData =
                    AmbulanceLocationRequest(
                        latitude = latitude,
                        longitude = longitude,
                        speed = speedKmh,
                        heading = headingValue,
                        emergency = emergencyActive
                    )

                RetrofitClient.api
                    .sendAmbulanceLocation(
                        ambulanceData
                    )
                    .enqueue(
                        object :
                            Callback<AmbulanceResponse> {

                            override fun onResponse(
                                call: Call<AmbulanceResponse>,
                                response: Response<AmbulanceResponse>
                            ) {

                                android.util.Log.d(
                                    "AMBULANCE_API",
                                    "Location sent: ${response.code()}"
                                )
                            }

                            override fun onFailure(
                                call: Call<AmbulanceResponse>,
                                t: Throwable
                            ) {

                                android.util.Log.e(
                                    "AMBULANCE_API",
                                    "Failed to send ambulance location",
                                    t
                                )
                            }
                        }
                    )
            }
        }

    // ---------------------------------------------------------
    // ACTIVITY CREATE
    // ---------------------------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        // Notification channel
        NotificationHelper.createChannel(this)

        // Android 13+ notification permission
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        // GPS client
        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)

        // Compose UI
        setContent {
            SmartAmbulanceApp()
        }

        // Check GPS permission
        checkLocationPermission()
    }

    // ---------------------------------------------------------
    // CHECK LOCATION PERMISSION
    // ---------------------------------------------------------

    private fun checkLocationPermission() {

        val finePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarsePermission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (
            finePermission ||
            coarsePermission
        ) {

            startGps()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ---------------------------------------------------------
    // START GPS
    // ---------------------------------------------------------

    private fun startGps() {

        val locationRequest =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000L
            )
                .setMinUpdateIntervalMillis(2000L)
                .build()

        try {

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )

        } catch (e: SecurityException) {

            gpsActive = false

            android.util.Log.e(
                "GPS",
                "Location permission error",
                e
            )
        }
    }

    // ---------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------

    private fun logoutAmbulance() {

        // Stop emergency
        emergencyActive = false

        // Mark ambulance offline
        val data = hashMapOf<String, Any>(
            "isActive" to false,
            "isAvailable" to false
        )

        firestore
            .collection("ambulances")
            .document(ambulanceId)
            .update(data)
            .addOnSuccessListener {

                android.util.Log.d(
                    "FIRESTORE_LOGOUT",
                    "Ambulance marked offline"
                )
            }
            .addOnFailureListener { error ->

                android.util.Log.e(
                    "FIRESTORE_LOGOUT",
                    "Could not update ambulance status",
                    error
                )
            }

        // Clear login session
        val prefs =
            getSharedPreferences(
                "SmartAmbulancePrefs",
                MODE_PRIVATE
            )

        prefs.edit()
            .clear()
            .apply()

        // Go to Welcome screen
        val intent =
            Intent(
                this@MainActivity,
                WelcomeActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }

    // ---------------------------------------------------------
    // ACTIVITY DESTROY
    // ---------------------------------------------------------

    override fun onDestroy() {

        super.onDestroy()

        fusedLocationClient
            .removeLocationUpdates(
                locationCallback
            )
    }

    // =========================================================
    // MAIN COMPOSE UI
    // =========================================================

    @Composable
    private fun SmartAmbulanceApp() {

        MaterialTheme {

            Surface(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    // -----------------------------------------
                    // TITLE
                    // -----------------------------------------

                    Text(
                        text = "🚑 Smart Ambulance",
                        fontSize = 28.sp
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Emergency Corridor System",
                        fontSize = 16.sp
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // -----------------------------------------
                    // GPS / HOSPITAL CARD
                    // -----------------------------------------

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(16.dp)
                        ) {

                            Text(
                                text =
                                    if (gpsActive)
                                        "🟢 GPS Active"
                                    else
                                        "🔴 GPS Inactive"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Text(
                                text =
                                    "Latitude: %.6f"
                                        .format(
                                            currentLatitude
                                        )
                            )

                            Text(
                                text =
                                    "Longitude: %.6f"
                                        .format(
                                            currentLongitude
                                        )
                            )

                            Text(
                                text =
                                    "Speed: %.1f km/h"
                                        .format(
                                            currentSpeed
                                        )
                            )

                            Text(
                                text =
                                    "Heading: %.0f°"
                                        .format(
                                            currentHeading
                                        )
                            )

                            // ---------------------------------
                            // HOSPITAL LOADING
                            // ---------------------------------

                            if (hospitalLoading) {

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(
                                    "🔎 Finding fastest nearby hospital..."
                                )
                            }

                            // ---------------------------------
                            // HOSPITAL RESULT
                            // ---------------------------------

                            if (fastestHospital != null) {

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Text(
                                    text =
                                        "🏥 ${fastestHospital!!.name}",
                                    fontSize = 18.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "🚗 %.2f km"
                                            .format(
                                                fastestHospital!!.distance_km
                                            )
                                )

                                Text(
                                    text =
                                        "⏱ %.1f min"
                                            .format(
                                                fastestHospital!!.duration_minutes
                                            )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(12.dp)
                                )

                                Button(
                                    onClick = {
                                        loadRouteToHospital()
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        if (routeLoading)
                                            "Loading Route..."
                                        else
                                            "🗺️ START ROUTE"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // -----------------------------------------
                    // MAP
                    // -----------------------------------------

                    AmbulanceMap()

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    // -----------------------------------------
                    // SPEED + DIRECTION
                    // -----------------------------------------

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Card(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(12.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text("Speed")

                                Text(
                                    "%.1f km/h"
                                        .format(
                                            currentSpeed
                                        ),
                                    fontSize = 20.sp
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Card(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(12.dp),

                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Text("Direction")

                                Text(
                                    directionName(
                                        currentHeading
                                    ),
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    // -----------------------------------------
                    // EMERGENCY BUTTON
                    // -----------------------------------------

                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),

                        onClick = {

                            emergencyActive =
                                !emergencyActive

                            if (emergencyActive) {

                                // Find fastest hospital
                                findFastestHospital()

                            } else {

                                // End emergency
                                fastestHospital = null

                                routeCoordinates =
                                    emptyList()
                            }
                        }
                    ) {

                        Text(
                            if (emergencyActive)
                                "🚨 END EMERGENCY"
                            else
                                "🚑 START EMERGENCY"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    // -----------------------------------------
                    // EMERGENCY INFORMATION
                    // -----------------------------------------

                    if (emergencyActive) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(16.dp)
                            ) {

                                Text(
                                    text =
                                        "🚨 EMERGENCY ACTIVE",
                                    fontSize = 20.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(6.dp)
                                )

                                Text(
                                    "500 m directional corridor active"
                                )

                                Text(
                                    "Nearby same-direction vehicles will be checked."
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    // -----------------------------------------
                    // LOGOUT
                    // -----------------------------------------

                    Button(
                        onClick = {
                            logoutAmbulance()
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text("🚪 Logout")
                    }
                }
            }
        }
    }

    // =========================================================
    // GOOGLE MAP
    // =========================================================

    @Composable
    private fun AmbulanceMap() {

        // ---------------------------------------------
        // WAITING FOR GPS
        // ---------------------------------------------

        if (
            currentLatitude == 0.0 &&
            currentLongitude == 0.0
        ) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxSize(),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        "Waiting for GPS location..."
                    )
                }
            }

            return
        }

        // ---------------------------------------------
        // AMBULANCE LOCATION
        // ---------------------------------------------

        val ambulanceLocation =
            LatLng(
                currentLatitude,
                currentLongitude
            )

        // ---------------------------------------------
        // CAMERA
        // ---------------------------------------------

        val cameraPositionState =
            rememberCameraPositionState()

        cameraPositionState.position =
            CameraPosition.fromLatLngZoom(
                ambulanceLocation,
                16f
            )

        // ---------------------------------------------
        // GOOGLE MAP
        // ---------------------------------------------

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
        ) {

            GoogleMap(
                modifier =
                    Modifier.fillMaxSize(),

                cameraPositionState =
                    cameraPositionState
            ) {

                // -----------------------------------------
                // AMBULANCE MARKER
                // -----------------------------------------

                Marker(
                    state =
                        MarkerState(
                            position =
                                ambulanceLocation
                        ),

                    title = "Ambulance",

                    snippet =
                        "Live ambulance location"
                )

                // -----------------------------------------
                // ROUTE POLYLINE
                // -----------------------------------------

                if (
                    routeCoordinates.size >= 2
                ) {

                    Polyline(
                        points =
                            routeCoordinates,

                        width = 12f
                    )
                }

                // -----------------------------------------
                // EMERGENCY CORRIDOR
                // -----------------------------------------

                if (emergencyActive) {

                    val corridorLength =
                        0.0045

                    val corridorWidth =
                        0.0008

                    val headingRadians =
                        Math.toRadians(
                            currentHeading
                        )

                    // Forward point
                    val forwardLat =
                        currentLatitude +
                                corridorLength *
                                kotlin.math.cos(
                                    headingRadians
                                )

                    val forwardLng =
                        currentLongitude +
                                corridorLength *
                                kotlin.math.sin(
                                    headingRadians
                                )

                    // Left start
                    val leftStart =
                        LatLng(
                            currentLatitude +
                                    corridorWidth *
                                    kotlin.math.cos(
                                        headingRadians +
                                                Math.PI / 2
                                    ),

                            currentLongitude +
                                    corridorWidth *
                                    kotlin.math.sin(
                                        headingRadians +
                                                Math.PI / 2
                                    )
                        )

                    // Left end
                    val leftEnd =
                        LatLng(
                            forwardLat +
                                    corridorWidth *
                                    kotlin.math.cos(
                                        headingRadians +
                                                Math.PI / 2
                                    ),

                            forwardLng +
                                    corridorWidth *
                                    kotlin.math.sin(
                                        headingRadians +
                                                Math.PI / 2
                                    )
                        )

                    // Right end
                    val rightEnd =
                        LatLng(
                            forwardLat +
                                    corridorWidth *
                                    kotlin.math.cos(
                                        headingRadians -
                                                Math.PI / 2
                                    ),

                            forwardLng +
                                    corridorWidth *
                                    kotlin.math.sin(
                                        headingRadians -
                                                Math.PI / 2
                                    )
                        )

                    // Right start
                    val rightStart =
                        LatLng(
                            currentLatitude +
                                    corridorWidth *
                                    kotlin.math.cos(
                                        headingRadians -
                                                Math.PI / 2
                                    ),

                            currentLongitude +
                                    corridorWidth *
                                    kotlin.math.sin(
                                        headingRadians -
                                                Math.PI / 2
                                    )
                        )

                    Polygon(
                        points =
                            listOf(
                                leftStart,
                                leftEnd,
                                rightEnd,
                                rightStart
                            ),

                        strokeWidth = 4f
                    )
                }
            }
        }
    }

    // =========================================================
    // DIRECTION NAME
    // =========================================================

    private fun directionName(
        heading: Double
    ): String {

        return when {

            heading >= 337.5 ||
                    heading < 22.5 ->
                "North"

            heading < 67.5 ->
                "North-East"

            heading < 112.5 ->
                "East"

            heading < 157.5 ->
                "South-East"

            heading < 202.5 ->
                "South"

            heading < 247.5 ->
                "South-West"

            heading < 292.5 ->
                "West"

            else ->
                "North-West"
        }
    }
}