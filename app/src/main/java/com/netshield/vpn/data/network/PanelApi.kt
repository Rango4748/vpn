package com.netshield.vpn.data.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * REST contract for a Marzban-style panel. Marzban is the most common self-hosted
 * Xray/V2Ray panel; its API shape (token auth + /api/user endpoints returning
 * subscription links) is also close enough to X-UI/3x-UI variants that the same
 * client can be reused with small per-panel adapters in PanelRepository.
 */
interface PanelApi {

    @FormUrlEncoded
    @POST("api/admin/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @GET("api/user/{username}")
    suspend fun getUser(
        @Header("Authorization") bearer: String,
        @retrofit2.http.Path("username") username: String
    ): Response<PanelUserResponse>

    @GET("api/system")
    suspend fun getSystemStats(
        @Header("Authorization") bearer: String
    ): Response<Any>

    companion object {
        fun create(baseUrl: String): PanelApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PanelApi::class.java)
        }
    }
}

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

data class PanelUserResponse(
    val username: String,
    val status: String,
    val used_traffic: Long,
    val data_limit: Long?,
    val expire: Long?,
    val subscription_url: String?,
    val links: List<String>?
)
