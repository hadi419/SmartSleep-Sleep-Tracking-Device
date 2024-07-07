package com.example.dreamcatch.network

data class user_password(val email: String, val password: String)

data class email(val email: String)

data class controlsignal(val email: String, val signal: Boolean)

data class analytics(val sleepscore: String, val sleptat:String, val wokeupat:String, val asleepfor:String, val rem:String, val temperature: String, val temperaturefeedback: String, val deepsleep: String)

data class alarmtime(val hour: Int, val min: Int)

data class scan(val email: String, val deviceid: String)

data class personal_info(val email: String?, val name: String, val gender: String, val age: String, val medical: String)

data class User(
    val email: String,
    val name: String,
    val gender: String,
    val age: String,
    val medical: String,
    val wakeup: String,
    val sleepat: String)

data class ApiResponse(val success: Boolean, val message: String?, val user: User?)

data class AnalyticsResponse(val analytics: analytics?)

