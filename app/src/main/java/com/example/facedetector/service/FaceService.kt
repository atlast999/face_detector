package com.example.facedetector.service

import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface FaceService {

    @Multipart
    @POST("api/upload/")
    fun uploadData(@Part file: MultipartBody.Part, @Part("name") name: RequestBody, @Part("id") id: RequestBody): Single<UploadResponse>

    @Multipart
    @POST("api/recognite/")
    fun recognite(@Part file: MultipartBody.Part): Single<Attendee>

    @POST("api/delete/")
    fun delete(@Body id: Int): Single<List<Attendee>>

    @GET("api/all_users")
    fun getAllUsers(): Single<List<Attendee>>
}