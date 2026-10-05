package com.smartambulance.app

data class AmbulanceLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double,
    val emergency: Boolean
)

data class AmbulanceResponse(
    val message: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val emergency: Boolean? = null
)

data class VehicleLocationRequest(
    val vehicle_id: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val heading: Double
)

data class VehicleCheckResponse(
    val alert: Boolean = false,
    val distance: Double? = null,
    val same_direction: Boolean? = null,
    val message: String? = null
)
data class FastestHospital(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance_km: Double,
    val duration_minutes: Double
)

data class FastestHospitalResponse(
    val success: Boolean = false,
    val ambulance: AmbulanceCoordinates? = null,
    val fastest_hospital: FastestHospital? = null,
    val hospitals: List<FastestHospital> = emptyList(),
    val message: String? = null
)

data class AmbulanceCoordinates(
    val latitude: Double,
    val longitude: Double
)
data class RouteResponse(
    val success: Boolean = false,
    val distance_km: Double? = null,
    val duration_minutes: Double? = null,
    val geometry: RouteGeometry? = null,
    val message: String? = null
)

data class RouteGeometry(
    val type: String? = null,
    val coordinates: List<List<Double>> = emptyList()
)