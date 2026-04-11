package com.rhythmgame.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.rhythmgame.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

sealed class DiscoveryState {
    data object Idle : DiscoveryState()
    data object Searching : DiscoveryState()
    data class Found(val url: String) : DiscoveryState()
    data object NotFound : DiscoveryState()
}

class ServerDiscovery(
    private val context: Context,
    private val prefs: SharedPreferences,
) {
    companion object {
        private const val TAG = "ServerDiscovery"
        private const val KEY_SERVER_URL = "server_url"
        private const val HEALTH_TIMEOUT_S = 5L
    }

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val healthClient = OkHttpClient.Builder()
        .connectTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(HEALTH_TIMEOUT_S, TimeUnit.SECONDS)
        .build()

    @Volatile
    var currentBaseUrl: String = BuildConfig.BASE_URL
        private set

    fun startDiscovery() {
        scope.launch {
            _state.value = DiscoveryState.Searching

            // 1. Manual override from Settings
            val manualUrl = prefs.getString(KEY_SERVER_URL, null)
            if (manualUrl != null && manualUrl != BuildConfig.BASE_URL) {
                if (checkHealth(manualUrl)) {
                    currentBaseUrl = manualUrl
                    _state.value = DiscoveryState.Found(manualUrl)
                    Log.d(TAG, "Manual URL healthy: $manualUrl")
                    return@launch
                }
            }

            // 2. Default URL from BuildConfig (Cloudflare tunnel URL in release)
            if (checkHealth(BuildConfig.BASE_URL)) {
                currentBaseUrl = BuildConfig.BASE_URL
                _state.value = DiscoveryState.Found(BuildConfig.BASE_URL)
                Log.d(TAG, "Default URL healthy: ${BuildConfig.BASE_URL}")
                return@launch
            }

            // 3. Emulator fallback (debug only)
            if (BuildConfig.DEBUG) {
                val emulatorUrl = "http://10.0.2.2:8000/api/"
                if (emulatorUrl != BuildConfig.BASE_URL && checkHealth(emulatorUrl)) {
                    currentBaseUrl = emulatorUrl
                    _state.value = DiscoveryState.Found(emulatorUrl)
                    Log.d(TAG, "Emulator fallback healthy")
                    return@launch
                }
            }

            // 4. Nothing reachable
            currentBaseUrl = BuildConfig.BASE_URL
            _state.value = DiscoveryState.NotFound
            Log.w(TAG, "No healthy server found")
        }
    }

    private fun checkHealth(baseUrl: String): Boolean {
        return try {
            val url = baseUrl.trimEnd('/').removeSuffix("/api") + "/api/health"
            val request = Request.Builder().url(url).get().build()
            val response = healthClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.d(TAG, "Health check failed for $baseUrl: ${e.message}")
            false
        }
    }

    fun setManualUrl(url: String) {
        if (url.isBlank() || url == BuildConfig.BASE_URL) {
            prefs.edit().remove(KEY_SERVER_URL).apply()
        } else {
            prefs.edit().putString(KEY_SERVER_URL, url).apply()
        }
        currentBaseUrl = url.ifBlank { BuildConfig.BASE_URL }
    }

    fun destroy() {
        scope.cancel()
    }
}
