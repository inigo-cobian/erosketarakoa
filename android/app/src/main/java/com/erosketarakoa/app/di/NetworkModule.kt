package com.erosketarakoa.app.di

import com.erosketarakoa.app.data.remote.PriceApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Gateway base URL. 10.0.2.2 is the host loopback from the Android emulator; override for
     * a real deployment. ponytail: hardcoded until a real backend host exists.
     */
    private const val GATEWAY_BASE_URL = "http://10.0.2.2:8000/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun providePriceApi(client: OkHttpClient, json: Json): PriceApi =
        Retrofit.Builder()
            .baseUrl(GATEWAY_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PriceApi::class.java)
}
