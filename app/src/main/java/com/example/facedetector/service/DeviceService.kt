package com.example.facedetector.service

import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface DeviceService {

    @GET("api/sensors")
    fun sensors(): Single<List<Device>>

    @POST("api/devices")
    fun sendCommand(@Body request: CommandRequest): Single<CommandResponse>

    @POST("user/login")
    fun login(@Body request: LoginRequest): Single<LoginResponse>


    @Multipart
    @POST("python/api/upload/")
    fun uploadData(@Part file: MultipartBody.Part, @Part("name") name: RequestBody, @Part("id") id: RequestBody): Single<UploadResponse>

    @Multipart
    @POST("python/api/recognite/")
    fun recognite(@Part file: MultipartBody.Part): Single<Attendee>

    @POST("python/api/delete/")
    fun delete(@Body request: DeleteRequest): Single<List<Attendee>>

    @GET("python/api/all_users/")
    fun getAllUsers(): Single<List<Attendee>>
}