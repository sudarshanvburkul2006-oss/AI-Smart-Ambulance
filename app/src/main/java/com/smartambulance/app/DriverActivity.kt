package com.smartambulance.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


// ============================================================
// NEARBY AMBULANCE DATA
// ============================================================

data class NearbyAmbulance(
    val ambulanceId: String,
    val vehicleNumber: String,
    val driverName: String,
    val hospitalName: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double
)


// ============================================================
// DRIVER ACTIVITY
// ============================================================

class DriverActivity : ComponentActivity() {

    // --------------------------------------------------------
    // FIRESTORE
    // --------------------------------------------------------

    private val firestore =
        FirebaseFirestore.getInstance()


    // --------------------------------------------------------
    // DRIVER VEHICLE ID
    // --------------------------------------------------------

    private var vehicleId by mutableStateOf("")


    // --------------------------------------------------------
    // DRIVER GPS
    // --------------------------------------------------------

    private var driverLatitude by mutableStateOf(0.0)

    private var driverLongitude by mutableStateOf(0.0)


    // --------------------------------------------------------
    // NEARBY AMBULANCE STATE
    // --------------------------------------------------------

    private var nearbyAmbulances by
    mutableStateOf<List<NearbyAmbulance>>(emptyList())

    private var searchingAmbulances by
    mutableStateOf(false)

    private var bookingMessage by
    mutableStateOf("")


    // --------------------------------------------------------
    // GPS CLIENT
    // --------------------------------------------------------

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient


    // --------------------------------------------------------
    // THIS TELLS THE PERMISSION RESULT WHAT TO DO
    // --------------------------------------------------------

    private var waitingForBookingLocation = false


    // --------------------------------------------------------
    // LOCATION PERMISSION
    // --------------------------------------------------------

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fine =
                permissions[
                    Manifest.permission.ACCESS_FINE_LOCATION
                ] == true

            val coarse =
                permissions[
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ] == true


            if (fine || coarse) {

                if (waitingForBookingLocation) {

                    waitingForBookingLocation = false

                    getDriverLocationAndFindAmbulances()

                } else {

                    startMonitoring()
                }

            } else {

                searchingAmbulances = false

                bookingMessage =
                    "Location permission is required."
            }
        }


    // ========================================================
    // ON CREATE
    // ========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


        // Initialize GPS client

        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(this)


        setContent {

            DriverDashboard(

                vehicleId = vehicleId,

                onVehicleIdChange = {
                    vehicleId = it
                },


                // Existing emergency monitoring

                onStartMonitoring = {
                    requestLocationAndStart()
                },


                // Nearby ambulance search

                onFindNearbyAmbulances = {
                    requestLocationAndFindAmbulances()
                },


                nearbyAmbulances =
                    nearbyAmbulances,


                searchingAmbulances =
                    searchingAmbulances,


                bookingMessage =
                    bookingMessage,


                // Booking request

                onRequestAmbulance = {
                        ambulance ->

                    requestAmbulance(
                        ambulance
                    )
                },


                // Home

                onHome = {
                    goToHome()
                },


                // Logout

                onLogout = {
                    logoutDriver()
                }
            )
        }
    }


    // ========================================================
    // REQUEST AMBULANCE
    // ========================================================

    private fun requestAmbulance(
        ambulance: NearbyAmbulance
    ) {

        // ----------------------------------------------------
        // CHECK VEHICLE ID
        // ----------------------------------------------------

        if (vehicleId.trim().isEmpty()) {

            bookingMessage =
                "Please enter your Vehicle ID first."

            return
        }


        // ----------------------------------------------------
        // CHECK DRIVER GPS
        // ----------------------------------------------------

        if (
            driverLatitude == 0.0 ||
            driverLongitude == 0.0
        ) {

            bookingMessage =
                "Driver location is not available. " +
                        "Please find nearby ambulances again."

            return
        }


        // ----------------------------------------------------
        // BOOKING DATA
        // ----------------------------------------------------

        val booking =
            hashMapOf<String, Any>(

                "vehicleId" to
                        vehicleId.trim(),

                "ambulanceId" to
                        ambulance.ambulanceId,

                "status" to
                        "REQUESTED",

                "vehicleLatitude" to
                        driverLatitude,

                "vehicleLongitude" to
                        driverLongitude,

                "ambulanceLatitude" to
                        ambulance.latitude,

                "ambulanceLongitude" to
                        ambulance.longitude,

                "distanceKm" to
                        ambulance.distanceKm,

                "createdAt" to
                        FieldValue.serverTimestamp()
            )


        // ----------------------------------------------------
        // CREATE FIRESTORE BOOKING
        // ----------------------------------------------------

        firestore
            .collection("ambulanceBookings")
            .add(booking)

            .addOnSuccessListener { document ->

                bookingMessage =
                    "🚑 Ambulance request sent successfully."


                android.util.Log.d(
                    "AMBULANCE_BOOKING",
                    "Booking created: ${document.id}"
                )
            }

            .addOnFailureListener { error ->

                bookingMessage =
                    "Booking failed: " +
                            (
                                    error.message
                                        ?: "Unknown error"
                                    )


                android.util.Log.e(
                    "AMBULANCE_BOOKING",
                    "Booking creation failed",
                    error
                )
            }
    }


    // ========================================================
    // FIND NEARBY AMBULANCES
    // ========================================================

    private fun findNearbyAmbulances(
        driverLatitude: Double,
        driverLongitude: Double
    ) {

        searchingAmbulances = true

        bookingMessage =
            "Searching nearby ambulances..."


        firestore
            .collection("ambulances")
            .whereEqualTo(
                "isActive",
                true
            )
            .get()

            .addOnSuccessListener { documents ->

                val results =
                    mutableListOf<NearbyAmbulance>()


                for (document in documents) {

                    // ----------------------------------------
                    // CHECK AVAILABILITY
                    // ----------------------------------------

                    val isAvailable =
                        document.getBoolean(
                            "isAvailable"
                        ) ?: false


                    if (!isAvailable) {
                        continue
                    }


                    // ----------------------------------------
                    // GET AMBULANCE GPS
                    // ----------------------------------------

                    val ambulanceLatitude =
                        document.getDouble(
                            "latitude"
                        ) ?: continue


                    val ambulanceLongitude =
                        document.getDouble(
                            "longitude"
                        ) ?: continue


                    // ----------------------------------------
                    // IGNORE INVALID LOCATION
                    // ----------------------------------------

                    if (
                        ambulanceLatitude == 0.0 ||
                        ambulanceLongitude == 0.0
                    ) {
                        continue
                    }


                    // ----------------------------------------
                    // CALCULATE DISTANCE
                    // ----------------------------------------

                    val distance =
                        FloatArray(1)


                    Location.distanceBetween(

                        driverLatitude,

                        driverLongitude,

                        ambulanceLatitude,

                        ambulanceLongitude,

                        distance
                    )


                    val distanceKm =
                        distance[0] / 1000.0


                    // ----------------------------------------
                    // ONLY SHOW WITHIN 5 KM
                    // ----------------------------------------

                    if (distanceKm <= 5.0) {

                        results.add(

                            NearbyAmbulance(

                                ambulanceId =
                                    document.getString(
                                        "ambulanceId"
                                    )
                                        ?: document.id,


                                vehicleNumber =
                                    document.getString(
                                        "vehicleNumber"
                                    )
                                        ?: "Unknown",


                                driverName =
                                    document.getString(
                                        "driverName"
                                    )
                                        ?: "Unknown",


                                hospitalName =
                                    document.getString(
                                        "hospitalName"
                                    )
                                        ?: "Unknown",


                                latitude =
                                    ambulanceLatitude,


                                longitude =
                                    ambulanceLongitude,


                                distanceKm =
                                    distanceKm
                            )
                        )
                    }
                }


                // ----------------------------------------
                // SORT NEAREST FIRST
                // ----------------------------------------

                nearbyAmbulances =
                    results.sortedBy {
                        it.distanceKm
                    }


                searchingAmbulances =
                    false


                // ----------------------------------------
                // RESULT MESSAGE
                // ----------------------------------------

                if (
                    nearbyAmbulances.isEmpty()
                ) {

                    bookingMessage =
                        "No available ambulance found within 5 km."

                } else {

                    bookingMessage =
                        "${nearbyAmbulances.size} " +
                                "available ambulance(s) found."
                }
            }


            .addOnFailureListener { error ->

                nearbyAmbulances =
                    emptyList()

                searchingAmbulances =
                    false

                bookingMessage =
                    "Unable to search ambulances: " +
                            (
                                    error.message
                                        ?: "Unknown Firestore error"
                                    )


                android.util.Log.e(
                    "AMBULANCE_SEARCH",
                    "Firestore search failed",
                    error
                )
            }
    }


    // ========================================================
    // REQUEST LOCATION AND FIND AMBULANCES
    // ========================================================

    private fun requestLocationAndFindAmbulances() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        // ----------------------------------------------------
        // REQUEST PERMISSION IF NEEDED
        // ----------------------------------------------------

        if (!fine && !coarse) {

            waitingForBookingLocation = true

            searchingAmbulances = true

            bookingMessage =
                "Please allow location permission."


            locationPermissionLauncher.launch(

                arrayOf(

                    Manifest.permission.ACCESS_FINE_LOCATION,

                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            return
        }


        // ----------------------------------------------------
        // GET LOCATION
        // ----------------------------------------------------

        getDriverLocationAndFindAmbulances()
    }


    // ========================================================
    // GET DRIVER LOCATION
    // ========================================================

    private fun getDriverLocationAndFindAmbulances() {

        searchingAmbulances = true

        bookingMessage =
            "Getting your current location..."


        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )

            .addOnSuccessListener { location ->

                if (location != null) {

                    driverLatitude =
                        location.latitude

                    driverLongitude =
                        location.longitude


                    android.util.Log.d(
                        "DRIVER_LOCATION",
                        "Latitude=$driverLatitude " +
                                "Longitude=$driverLongitude"
                    )


                    findNearbyAmbulances(

                        driverLatitude =
                            driverLatitude,

                        driverLongitude =
                            driverLongitude
                    )

                } else {

                    searchingAmbulances =
                        false

                    bookingMessage =
                        "Unable to get current location. " +
                                "Please try again."
                }
            }

            .addOnFailureListener { error ->

                searchingAmbulances =
                    false

                bookingMessage =
                    "Location error: " +
                            (
                                    error.message
                                        ?: "Unable to get location"
                                    )


                android.util.Log.e(
                    "DRIVER_LOCATION",
                    "Failed to get location",
                    error
                )
            }
    }


    // ========================================================
    // HOME
    // ========================================================

    private fun goToHome() {

        val intent =
            Intent(
                this,
                LoginActivity::class.java
            )


        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP


        startActivity(intent)

        finish()
    }


    // ========================================================
    // REQUEST LOCATION AND START MONITORING
    // ========================================================

    private fun requestLocationAndStart() {

        if (vehicleId.trim().isEmpty()) {
            return
        }


        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (fine || coarse) {

            startMonitoring()

        } else {

            waitingForBookingLocation = false


            locationPermissionLauncher.launch(

                arrayOf(

                    Manifest.permission.ACCESS_FINE_LOCATION,

                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    // ========================================================
    // START BACKGROUND DRIVER MONITORING
    // ========================================================

    private fun startMonitoring() {

        val intent =
            Intent(
                this,
                DriverLocationService::class.java
            )


        intent.putExtra(
            "VEHICLE_ID",
            vehicleId.trim()
        )


        ContextCompat.startForegroundService(
            this,
            intent
        )
    }


    // ========================================================
    // LOGOUT
    // ========================================================

    private fun logoutDriver() {

        // ----------------------------------------------------
        // STOP BACKGROUND MONITORING
        // ----------------------------------------------------

        val serviceIntent =
            Intent(
                this,
                DriverLocationService::class.java
            )


        stopService(
            serviceIntent
        )


        // ----------------------------------------------------
        // CLEAR LOGIN SESSION
        // ----------------------------------------------------

        val prefs =
            getSharedPreferences(
                "SmartAmbulancePrefs",
                MODE_PRIVATE
            )


        prefs.edit()
            .clear()
            .apply()


        // ----------------------------------------------------
        // RETURN TO WELCOME
        // ----------------------------------------------------

        val intent =
            Intent(
                this,
                WelcomeActivity::class.java
            )


        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK


        startActivity(intent)

        finish()
    }
}


// ============================================================
// DRIVER DASHBOARD
// ============================================================

@Composable
fun DriverDashboard(

    vehicleId: String,

    onVehicleIdChange:
        (String) -> Unit,

    onStartMonitoring:
        () -> Unit,

    onFindNearbyAmbulances:
        () -> Unit,

    nearbyAmbulances:
    List<NearbyAmbulance>,

    searchingAmbulances:
    Boolean,

    bookingMessage:
    String,

    onRequestAmbulance:
        (NearbyAmbulance) -> Unit,

    onHome:
        () -> Unit,

    onLogout:
        () -> Unit
) {

    var errorMessage by
    remember {
        mutableStateOf("")
    }


    MaterialTheme {

        Surface(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center

            ) {


                // ====================================================
                // TITLE
                // ====================================================

                Text(
                    text =
                        "🚗 Driver Dashboard",

                    fontSize =
                        28.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Connected Vehicle Monitoring",

                    fontSize =
                        16.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )


                // ====================================================
                // VEHICLE CARD
                // ====================================================

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {

                        Text(
                            text =
                                "Vehicle Registration",

                            fontSize =
                                22.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        Text(
                            text =
                                "Enter your registered vehicle ID."
                        )


                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        OutlinedTextField(

                            value =
                                vehicleId,

                            onValueChange = {

                                onVehicleIdChange(it)

                                errorMessage = ""
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            label = {
                                Text("Vehicle ID")
                            },

                            placeholder = {
                                Text("Example: CAR-001")
                            },

                            singleLine = true
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        if (
                            errorMessage.isNotEmpty()
                        ) {

                            Text(
                                text =
                                    errorMessage,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }


                        // ------------------------------------------------
                        // EMERGENCY MONITORING
                        // ------------------------------------------------

                        Button(

                            onClick = {

                                if (
                                    vehicleId
                                        .trim()
                                        .isEmpty()
                                ) {

                                    errorMessage =
                                        "Please enter your Vehicle ID."

                                } else {

                                    onStartMonitoring()
                                }
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                text =
                                    "🟢 Enable Emergency Monitoring"
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // ====================================================
                // MONITORING INFORMATION
                // ====================================================

                Text(
                    text =
                        "After monitoring starts, you can close the app. " +
                                "The background service will continue monitoring."
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "🔐 Driver authentication required"
                )


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                // ====================================================
                // AMBULANCE BOOKING
                // ====================================================

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(20.dp)
                    ) {


                        Text(
                            text =
                                "🚑 Ambulance Booking",

                            fontSize =
                                22.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )


                        Text(
                            text =
                                "Find an available ambulance near your current location."
                        )


                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )


                        // ------------------------------------------------
                        // FIND BUTTON
                        // ------------------------------------------------

                        Button(

                            onClick = {
                                onFindNearbyAmbulances()
                            },

                            modifier =
                                Modifier.fillMaxWidth(),

                            enabled =
                                !searchingAmbulances

                        ) {

                            Text(

                                text =
                                    if (
                                        searchingAmbulances
                                    ) {

                                        "🔄 Searching..."

                                    } else {

                                        "🔎 Find Nearby Ambulances"
                                    }
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )


                        // ------------------------------------------------
                        // MESSAGE
                        // ------------------------------------------------

                        if (
                            bookingMessage.isNotEmpty()
                        ) {

                            Text(
                                text =
                                    bookingMessage
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )
                        }


                        // ====================================================
                        // AMBULANCE RESULTS
                        // ====================================================

                        nearbyAmbulances.forEach { ambulance ->

                            Card(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            bottom = 10.dp
                                        )
                            ) {

                                Column(
                                    modifier =
                                        Modifier.padding(16.dp)
                                ) {


                                    Text(
                                        text =
                                            "🚑 ${ambulance.ambulanceId}",

                                        fontSize =
                                            18.sp
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.height(4.dp)
                                    )


                                    Text(
                                        text =
                                            "Vehicle: " +
                                                    ambulance.vehicleNumber
                                    )


                                    Text(
                                        text =
                                            "Driver: " +
                                                    ambulance.driverName
                                    )


                                    Text(
                                        text =
                                            "Hospital: " +
                                                    ambulance.hospitalName
                                    )


                                    Text(
                                        text =
                                            String.format(
                                                "Distance: %.2f km",
                                                ambulance.distanceKm
                                            )
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.height(10.dp)
                                    )


                                    // ------------------------------------------------
                                    // REQUEST AMBULANCE
                                    // ------------------------------------------------

                                    Button(

                                        onClick = {

                                            onRequestAmbulance(
                                                ambulance
                                            )
                                        },

                                        modifier =
                                            Modifier.fillMaxWidth()

                                    ) {

                                        Text(
                                            text =
                                                "🚑 REQUEST AMBULANCE"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                // ====================================================
                // HOME
                // ====================================================

                OutlinedButton(

                    onClick = {
                        onHome()
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(
                        text =
                            "🏠 Home"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // ====================================================
                // LOGOUT
                // ====================================================

                OutlinedButton(

                    onClick = {
                        onLogout()
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(
                        text =
                            "🚪 Logout"
                    )
                }
            }
        }
    }
}