package com.rhythmgame.navigation

import java.net.URLEncoder

object NavRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val SONG_LIST = "song_list"
    const val UPLOAD = "upload"
    const val RECORD = "record"
    const val AUDIO_EDIT = "audio_edit/{filePath}"
    const val SETTINGS = "settings"
    const val RANKED = "ranked"
    const val PROFILE = "profile"
    const val MULTIPLAYER = "multiplayer"
    const val STORE = "store"
    const val SONG_DETAIL = "song_detail/{songId}"
    const val GAME = "game/{songId}/{difficulty}"
    const val RESULT = "result/{songId}/{difficulty}/{score}/{maxCombo}/{perfect}/{great}/{good}/{miss}/{overpress}"

    fun songDetail(songId: String) = "song_detail/$songId"
    fun audioEdit(filePath: String) = "audio_edit/${URLEncoder.encode(filePath, "UTF-8")}"
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
        overpress: Int = 0,
    ) = "result/$songId/$difficulty/$score/$maxCombo/$perfect/$great/$good/$miss/$overpress"
}
