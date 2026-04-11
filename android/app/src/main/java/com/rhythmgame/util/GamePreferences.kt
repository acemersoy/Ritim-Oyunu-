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

    companion object {
        const val STYLE_HIGHWAY = "highway"
        const val STYLE_ARC = "arc"
    }
}
