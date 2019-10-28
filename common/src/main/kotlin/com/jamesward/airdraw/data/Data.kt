package com.jamesward.airdraw.data

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

@Serializable
data class Orientation(val azimuth: Float, val pitch: Float, val timestamp: Long)

fun MutableList<Orientation>.json(): String {
    val json = Json(JsonConfiguration.Stable)
    return json.stringify(Orientation.serializer().list, this.toList())
}

@Serializable
data class LabelAnnotation(val description: String, val score: Float)

@Serializable
data class ImageResult(val image: ByteArray, val labelAnnotations: List<LabelAnnotation>) {
    fun json(): String {
        val json = Json(JsonConfiguration.Stable)
        return json.stringify(serializer(), this)
    }

    companion object {
        fun fromJson(s: String): ImageResult? {
            val json = Json(JsonConfiguration.Stable)
            return json.parse(serializer(), s)
        }
    }
}
