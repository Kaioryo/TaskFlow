package com.taskflow.app

import android.content.Context
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://zenquotes.io/api/"
    private const val CACHE_SIZE = 10 * 1024 * 1024L // 10 MB

    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // ✅ CACHE INTERCEPTOR - Hanya cache saat offline
    private val cacheInterceptor = Interceptor { chain ->
        var request = chain.request()

        if (!NetworkHelper(context).isNetworkAvailable()) {
            // ✅ OFFLINE: Gunakan cache
            val cacheControl = CacheControl.Builder()
                .maxStale(7, TimeUnit.DAYS)
                .onlyIfCached()
                .build()

            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        } else {
            // ✅ ONLINE: NO CACHE - Force network
            val cacheControl = CacheControl.Builder()
                .noCache()
                .noStore() // ✅ Jangan simpan ke cache
                .build()

            request = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
        }

        chain.proceed(request)
    }

    private val cache: Cache by lazy {
        val cacheDir = File(context.cacheDir, "http_cache")
        Cache(cacheDir, CACHE_SIZE)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor(cacheInterceptor) // ✅ HANYA 1 interceptor
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val quoteApiService: QuoteApiService by lazy {
        retrofit.create(QuoteApiService::class.java)
    }

    fun clearCache() {
        try {
            cache.evictAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
