package com.valuai.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Dns
import java.net.InetAddress

// --- Data class-ok ---

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val access_token: String, val token_type: String)

data class RegisterRequest(val email: String, val password: String, val full_name: String = "")
data class RegisterResponse(val id: String, val email: String, val message: String = "Registration successful")

data class EstimationResult(
    val item_name: String = "",
    val category: String = "",
    val condition: String = "",
    val condition_notes: String = "",
    val price_min: Double = 0.0,
    val price_max: Double = 0.0,
    val price_recommended: Double = 0.0,
    val currency: String = "USD",
    val summary: String = "",
    val market_references_count: Int = 0,
    val confidence: String = ""
)

data class EstimationResponse(
    val id: String,
    val created_at: String,
    val description: String,
    val status: String = "done",
    val result: EstimationResult?
)

// --- API interface ---

interface ValuAIApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @Multipart
    @POST("estimations/")
    suspend fun createEstimation(
        @Header("Authorization") token: String,
        @Part images: List<MultipartBody.Part>,
        @Part("description") description: RequestBody,
        @Part("currency") currency: RequestBody
    ): Response<EstimationResponse>

    @GET("estimations/")
    suspend fun getEstimations(
        @Header("Authorization") token: String
    ): Response<List<EstimationResponse>>
}

// --- Retrofit instance ---

object ApiClient {
    private const val BASE_URL = "https://api.akarmilofasz.com/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return if (hostname == "api.akarmilofasz.com") {
                listOf(InetAddress.getByName("104.209.94.60"))
            } else {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .dns(customDns)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api: ValuAIApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ValuAIApi::class.java)
    }
}