package com.example.facedetector.service

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DeviceService {

    @GET("api/sensors")
    fun sensors(): Single<List<Device>>

    @POST("api/devices")
    fun sendCommand(@Body request: CommandRequest): Completable
}