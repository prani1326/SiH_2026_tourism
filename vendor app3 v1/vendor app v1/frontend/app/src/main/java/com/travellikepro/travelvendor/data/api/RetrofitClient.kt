package com.travellikepro.travelvendor.data.api

import android.content.Context
import com.travellikepro.travelvendor.BuildConfig
import com.travellikepro.travelvendor.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = BuildConfig.API_BASE_URL

    private var vendorApi: VendorApi? = null

    fun getApi(context: Context): VendorApi {
        if (vendorApi == null) {
            synchronized(this) {
                if (vendorApi == null) {
                    val tokenManager = TokenManager(context.applicationContext)
                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    val authInterceptor = AuthInterceptor(tokenManager)

                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(authInterceptor)
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    vendorApi = retrofit.create(VendorApi::class.java)
                }
            }
        }
        return vendorApi!!
    }
}