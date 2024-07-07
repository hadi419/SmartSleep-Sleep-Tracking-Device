package com.example.dreamcatch.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @POST("login")
    fun login(@Body loginRequest: user_password): Call<ApiResponse>
    @POST("signup")
    fun signup(@Body signupRequest: user_password): Call<ApiResponse>
    @POST("personal_info")
    fun personal_info(@Body personal_infoRequest: personal_info): Call<ApiResponse>
    @POST("start_stop")
    fun start_stop(@Body start_stopRequest: controlsignal): Call<ApiResponse>
    @POST("analytics")
    fun analytics(@Body analyticsRequest: email): Call<AnalyticsResponse>
    @POST("alarmtime")
    fun alarmtime(@Body alarmtimeRequest: email): Call<alarmtime>
    @POST("device")
    fun device(@Body deviceRequest: scan): Call<ApiResponse>

}