package org.traccar.client.trailblazer.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.traccar.client.trailblazer.data.database.ImageMetadata
import org.traccar.client.trailblazer.data.database.ImageResponse
import org.traccar.client.trailblazer.model.DeviceResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/images")
    suspend fun uploadMetadata(@Body metadata: ImageMetadata): Response<ImageResponse>

    @GET("api/devices")
    suspend fun getDeviceBySerial(
        @Query("uniqueId") uniqueId: String
    ): Response<List<DeviceResponse>>

        @POST("api/images/{id}/upload")
        suspend fun uploadImage(
            @Path("id") imageId: Int,
            @Body image: RequestBody
        ): Response<ResponseBody>



}

