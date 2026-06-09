package com.example.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FirestoreValue(
    val stringValue: String? = null,
    val integerValue: String? = null,
    val doubleValue: Double? = null,
    val booleanValue: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreDocumentRequest(
    val fields: Map<String, FirestoreValue>
)

@JsonClass(generateAdapter = true)
data class FirestoreDocumentResponse(
    val name: String? = null,
    val fields: Map<String, FirestoreValue>? = null,
    val createTime: String? = null,
    val updateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreListResponse(
    val documents: List<FirestoreDocumentResponse>? = null
)

interface FirestoreApiService {
    @POST("v1/projects/{projectId}/databases/(default)/documents/{collectionId}")
    suspend fun createDocument(
        @Path("projectId") projectId: String,
        @Path("collectionId") collectionId: String,
        @Query("key") apiKey: String,
        @Body document: FirestoreDocumentRequest
    ): FirestoreDocumentResponse

    @GET("v1/projects/{projectId}/databases/(default)/documents/{collectionId}")
    suspend fun listDocuments(
        @Path("projectId") projectId: String,
        @Path("collectionId") collectionId: String,
        @Query("key") apiKey: String,
        @Query("pageSize") pageSize: Int? = 100
    ): FirestoreListResponse

    @DELETE("v1/{documentPath}")
    suspend fun deleteDocument(
        @Path(value = "documentPath", encoded = true) documentPath: String,
        @Query("key") apiKey: String
    )
}

object FirestoreRetrofitClient {
    private const val BASE_URL = "https://firestore.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val service: FirestoreApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(FirestoreApiService::class.java)
    }
}
