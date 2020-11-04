package com.jamesward.airdraw.data

data class Orientation(val azimuth: Float, val pitch: Float, val timestamp: Long)

data class LabelAnnotation(val description: String, val score: Float)

data class ImageResult(val image: ByteArray, val labelAnnotations: Array<LabelAnnotation>)
