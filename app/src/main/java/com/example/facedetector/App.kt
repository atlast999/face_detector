package com.example.facedetector

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.facedetector.service.FaceService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class App : Application(){

    override fun onCreate() {
        super.onCreate()
        val pref = applicationContext.getSharedPreferences("app", Context.MODE_PRIVATE)
        setUpPreferences(pref)
    }

    companion object{
        private var preferences: SharedPreferences? = null
        private var faceService: FaceService? = null
        fun setUpPreferences(pref: SharedPreferences){
            preferences = pref
        }

        fun getPref() = preferences

        fun getFaceService(): FaceService {
            if (faceService == null){
                faceService = buildFaceService()
            }
            return faceService!!
        }

        private fun buildFaceService(): FaceService {
            val gson: Gson = GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
            val baseUrl = preferences?.getString("baseUrl", "http://192.168.42.156:8000") ?: "http://192.168.42.156:8000"
            val retrofit =  Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(defaultOkHttpClient())
                .build()
            return  retrofit.create(FaceService::class.java)
        }

        private fun defaultOkHttpClient(): OkHttpClient {
            val httpLoggingInterceptor =  HttpLoggingInterceptor()
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            builder.interceptors().add(httpLoggingInterceptor)
            builder.readTimeout(30, TimeUnit.SECONDS)
            builder.connectTimeout(30, TimeUnit.SECONDS)
            return builder.build()
        }
    }

}