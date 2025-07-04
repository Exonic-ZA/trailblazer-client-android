package org.traccar.client.trailblazer.api

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.traccar.client.trailblazer.ui.Trailblazer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://pathfinder.sbmkinetics.co.za/" // Staging

    // Fallback credentials for development/testing
    private const val FALLBACK_USERNAME = "wesley@exonic.co.za"
    private const val FALLBACK_PASSWORD = "Pass123!"

    fun create(trailblazer: Trailblazer, username: String? = null, password: String? = null): ApiService {
        // Use provided credentials, or fall back to defaults
        val finalUsername = username ?: FALLBACK_USERNAME
        val finalPassword = password ?: FALLBACK_PASSWORD

        val credentials = Credentials.basic(finalUsername, finalPassword)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", credentials)
                    .addHeader("Accept", "*/*")
                    .addHeader("Content-Type", "application/json; charset=UTF-8")
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}