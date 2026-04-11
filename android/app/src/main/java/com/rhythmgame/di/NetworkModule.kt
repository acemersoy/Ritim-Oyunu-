package com.rhythmgame.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.rhythmgame.BuildConfig
import com.rhythmgame.data.remote.RhythmGameApi
import com.rhythmgame.network.ServerDiscovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PREFS_NAME = "rhythm_game_prefs"

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideServerDiscovery(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
    ): ServerDiscovery {
        return ServerDiscovery(context, prefs)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(serverDiscovery: ServerDiscovery): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .addInterceptor { chain ->
                val original = chain.request()
                val discoveredUrl = serverDiscovery.currentBaseUrl.toHttpUrlOrNull()
                if (discoveredUrl != null) {
                    val newUrl = original.url.newBuilder()
                        .scheme(discoveredUrl.scheme)
                        .host(discoveredUrl.host)
                        .port(discoveredUrl.port)
                        .build()
                    chain.proceed(original.newBuilder().url(newUrl).build())
                } else {
                    chain.proceed(original)
                }
            }
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRhythmGameApi(retrofit: Retrofit): RhythmGameApi {
        return retrofit.create(RhythmGameApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
