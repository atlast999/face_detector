package com.example.facedetector.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Attendee(
    @SerializedName("name") @Expose val name: String?,
    @SerializedName("attendId") @Expose val attendId: Int
)