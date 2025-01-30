package org.traccar.client.trailblazer.data.database

data class ImageMetadata(
    val fileName: String,
    val fileExtension: String,
    val deviceId: CharSequence,
    val latitude: Double,
    val longitude: Double
)

data class ImageResponse(
    val id: Int
)
