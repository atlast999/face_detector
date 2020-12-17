package com.example.facedetector.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class DeleteRequest(
    @SerializedName("id") @Expose val id: Int
)