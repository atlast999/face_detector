package com.example.facedetector.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Device(
    @SerializedName("temperature") @Expose val temperature: String,
    @SerializedName("humidity") @Expose val humidity: String
)

data class CommandRequest(
    @SerializedName("command") @Expose val command: String
)