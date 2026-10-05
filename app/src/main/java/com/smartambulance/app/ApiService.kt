package com.smartambulance.app

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @POST("ambulance/location")
    fun sendAmbulanceLocation(
        @Body data: AmbulanceLocationRequest
    ): Call<AmbulanceResponse>

    @POST("vehicle/check")
    fun checkVehicle(
        @Body data: VehicleLocationRequest
    ): Call<VehicleCheckResponse>

    @GET("hospitals/fastest")
    fun getFastestHospital(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radius") radius: Int = 10000
    ): Call<FastestHospitalResponse>

    @GET("route")
    fun getRoute(
        @Query("start_lat") startLat: Double,
        @Query("start_lon") startLon: Double,
        @Query("end_lat") endLat: Double,
        @Query("end_lon") endLon: Double
    ): Call<RouteResponse>
}