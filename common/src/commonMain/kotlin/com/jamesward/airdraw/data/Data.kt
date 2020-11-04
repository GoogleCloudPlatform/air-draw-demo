package com.jamesward.airdraw.data

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class Orientation(val azimuth: Float, val pitch: Float, val timestamp: Long)

fun MutableList<Orientation>.json(): String {
    return Json.encodeToString(this.toList())
}

@Serializable
data class LabelAnnotation(val description: String, val score: Float)

@Serializable
data class ImageResult(val image: ByteArray, val labelAnnotations: List<LabelAnnotation>)
