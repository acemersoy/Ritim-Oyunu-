package com.rhythmgame.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val UPLOAD = "upload"
    const val SETTINGS = "settings"
    const val SONG_DETAIL = "song_detail/{songId}"
    const val GAME = "game/{songId}/{difficulty}"
    const val RESULT = "result/{songId}/{difficulty}/{score}/{maxCombo}/{perfect}/{great}/{good}/{miss}"

    fun songDetail(songId: String) = "song_detail/$songId"
    fun game(songId: String, difficulty: String) = "game/$songId/$difficulty"
    fun result(
        songId: String,
        difficulty: String,
        score: Int,
        maxCombo: Int,
        perfect: Int,
        great: Int,
        good: Int,
        miss: Int,
    ) = "result/$songId/$difficulty/$score/$maxCombo/$perfect/$great/$good/$miss"
}
