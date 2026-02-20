package com.rhythmgame.util

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCalibration @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("audio_calibration", Context.MODE_PRIVATE)

    var offsetMs: Long
        get() = prefs.getLong("offset_ms", DEFAULT_OFFSET_MS)
        set(value) = prefs.edit().putLong("offset_ms", value).apply()

    companion object {
        const val DEFAULT_OFFSET_MS = -80L
        const val MIN_OFFSET_MS = -500L
        const val MAX_OFFSET_MS = 500L
    }
}
