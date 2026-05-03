package com.rhythmgame.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamePreferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("game_preferences", Context.MODE_PRIVATE)

    var gameScreenStyle: String
        get() = prefs.getString("game_screen_style", STYLE_HIGHWAY) ?: STYLE_HIGHWAY
        set(value) = prefs.edit().putString("game_screen_style", value).apply()

    var particlesEnabled: Boolean
        get() = prefs.getBoolean("particles_enabled", true)
        set(value) = prefs.edit().putBoolean("particles_enabled", value).apply()

    var rhythmVibrationEnabled: Boolean
        get() = prefs.getBoolean("rhythm_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("rhythm_vibration_enabled", value).apply()

    var menuVibrationEnabled: Boolean
        get() = prefs.getBoolean("menu_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("menu_vibration_enabled", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("music_volume", 0.8f)
        set(value) = prefs.edit().putFloat("music_volume", value).apply()

    var sfxVolume: Float
        get() = prefs.getFloat("sfx_volume", 0.7f)
        set(value) = prefs.edit().putFloat("sfx_volume", value).apply()

    companion object {
        const val STYLE_HIGHWAY = "highway"
        const val STYLE_ARC = "arc"
    }
}
