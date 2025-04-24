package org.traccar.client.trailblazer.api

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.traccar.client.trailblazer.ui.Trailblazer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://trailblazer.sbmkinetics.co.za/"
    //TODO: Move to a secure space. Ran out of time.
    private const val USERNAME = "system@trailblazer.internal"
    private const val PASSWORD = "Babbling+Stomp+Bottling8+Payroll"

    fun create(trailblazer: Trailblazer): ApiService {
        val credentials = Credentials.basic(USERNAME, PASSWORD)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", credentials) // Direct Basic Auth
                    .addHeader("Accept", "*/*") // Accept all response types
                    .addHeader("Content-Type", "application/json; charset=UTF-8") // JSON content type
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
