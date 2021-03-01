package com.example.facedetector.data

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

class DataHelper {
    companion object {

        fun confirmUser(
            embeddings: FloatArray,
            userFeatureId: String,
            userFeatureData: UserFeatureData
        ): Boolean {
            val features = userFeatureData.data.find { it.id == userFeatureId }?.features
            var average = 0.0
            features?.forEach {
                var distance = 0.0

                for (index in it.indices) {
                    distance += (embeddings[index] - it[index]) * (embeddings[index] - it[index])
                }
                distance = sqrt(distance)


                average += distance
            }
            if (features != null) {
                average /= features.size
            }
            Log.d("TAG", "confirmUser: $userFeatureId : $average: ${features?.size}")
            return average < 9.5
        }

        fun checkData(userFeatureData: UserFeatureData) {
            userFeatureData.data.forEach {
                val length = it.features.size
                for (i in 0 until length - 1) {
                    for (j in i + 1 until length) {
                        var distance = 0.0
                        for (index in it.features[i].indices) {
                            distance += (it.features[j][index] - it.features[i][index]).toDouble()
                                .pow(2.0)
                        }
                        distance = sqrt(distance)
                        Log.d("TAG", "user: ${it.id} - features: $i : $j - distance: $distance")
                    }
                }
            }

        }

    }

}
data class UserFeatureData(
    val num_user: Int,
    val data: List<UserFeature>
)

data class UserFeature(
    val id: String,
    val name: String,
    val features: List<List<Float>>
)
