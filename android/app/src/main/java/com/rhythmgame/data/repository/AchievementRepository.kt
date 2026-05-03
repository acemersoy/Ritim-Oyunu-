package com.rhythmgame.data.repository

import com.rhythmgame.data.local.AchievementDao
import com.rhythmgame.data.local.AchievementEntity
import com.rhythmgame.data.local.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao,
    private val userDao: UserDao,
) {
    val achievements: Flow<List<AchievementEntity>> = achievementDao.getAll()
    val unlockedCount: Flow<Int> = achievementDao.getUnlockedCount()

    suspend fun seedAchievements() {
        val seeds = listOf(
            // Easy (10)
            AchievementEntity("first_game", "Ilk Adim", "1 oyun oyna", "easy", target = 1),
            AchievementEntity("five_games", "Caylak", "5 oyun oyna", "easy", target = 5),
            AchievementEntity("first_perfect", "Mukemmel", "1 PERFECT hit al", "easy", target = 1),
            AchievementEntity("ten_combo", "Kombocu", "10 combo yap", "easy", target = 10),
            AchievementEntity("first_upload", "DJ", "1 sarki yukle", "easy", target = 1),
            AchievementEntity("easy_complete", "Isinma", "Easy zorluk tamamla", "easy", target = 1),
            AchievementEntity("medium_complete", "Orta Yol", "Medium zorluk tamamla", "easy", target = 1),
            AchievementEntity("grade_c", "Gecti", "C notu al", "easy", target = 1),
            AchievementEntity("grade_b", "Fena Degil", "B notu al", "easy", target = 1),
            AchievementEntity("first_coins", "Zenginlik Yolu", "Ilk parayi kazan", "easy", target = 1),
            // Medium (5)
            AchievementEntity("fifty_games", "Deneyimli", "50 oyun oyna", "medium", target = 50),
            AchievementEntity("hundred_combo", "Kombo Ustasi", "100 combo yap", "medium", target = 100),
            AchievementEntity("grade_a", "Yetenekli", "A notu al", "medium", target = 1),
            AchievementEntity("hard_complete", "Cesaretli", "Hard zorluk tamamla", "medium", target = 1),
            AchievementEntity("five_songs", "Koleksiyoncu", "5 farkli sarki oyna", "medium", target = 5),
            // Hard (10)
            AchievementEntity("grade_s", "Efsane", "S notu al", "hard", target = 1),
            AchievementEntity("grade_s_hard", "Tanri", "Hard zorlukta S notu al", "hard", target = 1),
            AchievementEntity("full_perfect", "Kusursuz", "%100 Perfect al", "hard", target = 1),
            AchievementEntity("five_hundred_combo", "Durdurulamaz", "500 combo yap", "hard", target = 500),
            AchievementEntity("hundred_games", "Bagimli", "100 oyun oyna", "hard", target = 100),
            AchievementEntity("thousand_perfects", "Parmak Ustasi", "Toplam 1000 Perfect hit", "hard", target = 1000),
            AchievementEntity("power_master", "Guc Ustasi", "50 kez power aktive et", "hard", target = 50),
            AchievementEntity("all_difficulties", "Her Yol", "Ayni sarkiyi 3 zorlukta bitir", "hard", target = 1),
            AchievementEntity("marathon", "Maraton", "1 gunde 10 oyun oyna", "hard", target = 10),
            AchievementEntity("coin_hoarder", "Hazine Avcisi", "Toplam 500 coin biriktir", "hard", target = 500),
        )
        achievementDao.insertAll(seeds)
    }

    /**
     * Check all achievement conditions and unlock any that are met.
     * Returns the list of achievements that were NEWLY unlocked in this call.
     */
    suspend fun checkAndUnlock(
        songId: String,
        difficulty: String,
        accuracy: Float,
        maxCombo: Int,
        perfectCount: Int,
        totalNotes: Int,
        coinsEarned: Int,
    ): List<AchievementEntity> {
        val newlyUnlockedIds = mutableListOf<String>()

        val grade = when {
            accuracy >= 95f -> "S"
            accuracy >= 85f -> "A"
            accuracy >= 75f -> "B"
            accuracy >= 65f -> "C"
            else -> "D"
        }

        val results = userDao.getAllResults().first()
        val totalGames = results.size + 1 // including current game

        // Easy achievements
        tryUnlock("first_game", totalGames, 1, newlyUnlockedIds)
        tryUnlock("five_games", totalGames, 5, newlyUnlockedIds)

        if (perfectCount > 0) tryUnlock("first_perfect", 1, 1, newlyUnlockedIds)
        tryUnlock("ten_combo", maxCombo, 10, newlyUnlockedIds)

        when (difficulty) {
            "easy" -> tryUnlock("easy_complete", 1, 1, newlyUnlockedIds)
            "medium" -> tryUnlock("medium_complete", 1, 1, newlyUnlockedIds)
            "hard" -> tryUnlock("hard_complete", 1, 1, newlyUnlockedIds)
        }

        if (grade <= "C") tryUnlock("grade_c", 1, 1, newlyUnlockedIds)
        if (grade <= "B") tryUnlock("grade_b", 1, 1, newlyUnlockedIds)
        if (grade <= "A") tryUnlock("grade_a", 1, 1, newlyUnlockedIds)
        if (grade == "S") tryUnlock("grade_s", 1, 1, newlyUnlockedIds)
        if (grade == "S" && difficulty == "hard") tryUnlock("grade_s_hard", 1, 1, newlyUnlockedIds)

        if (coinsEarned > 0) tryUnlock("first_coins", 1, 1, newlyUnlockedIds)

        // Medium
        tryUnlock("fifty_games", totalGames, 50, newlyUnlockedIds)
        tryUnlock("hundred_combo", maxCombo, 100, newlyUnlockedIds)

        val uniqueSongs = results.map { it.songId }.toSet().size + 1
        tryUnlock("five_songs", uniqueSongs, 5, newlyUnlockedIds)

        // Hard
        if (perfectCount == totalNotes && totalNotes > 0) tryUnlock("full_perfect", 1, 1, newlyUnlockedIds)
        tryUnlock("five_hundred_combo", maxCombo, 500, newlyUnlockedIds)
        tryUnlock("hundred_games", totalGames, 100, newlyUnlockedIds)

        // Cumulative perfects
        val totalPerfects = results.sumOf { ((it.accuracy / 100f) * 1).toInt() } + perfectCount
        tryUnlock("thousand_perfects", totalPerfects, 1000, newlyUnlockedIds)

        // All difficulties for same song
        val songResults = results.filter { it.songId == songId }.map { it.difficulty }.toSet() + difficulty
        if (songResults.containsAll(listOf("easy", "medium", "hard"))) {
            tryUnlock("all_difficulties", 1, 1, newlyUnlockedIds)
        }

        // Marathon: 10 games today
        val todayStart = System.currentTimeMillis() - (System.currentTimeMillis() % 86_400_000L)
        val gamesToday = results.count { it.playedAt >= todayStart } + 1
        tryUnlock("marathon", gamesToday, 10, newlyUnlockedIds)

        // Coin hoarder - check profile coins
        val profile = userDao.getProfileSync()
        if (profile != null) {
            tryUnlock("coin_hoarder", profile.coins.toInt(), 500, newlyUnlockedIds)
        }

        return newlyUnlockedIds.mapNotNull { achievementDao.getById(it) }
    }

    private suspend fun tryUnlock(
        id: String,
        current: Int,
        target: Int,
        newlyUnlockedIds: MutableList<String>,
    ) {
        achievementDao.updateProgress(id, current.coerceAtMost(target))
        if (current >= target) {
            val rows = achievementDao.unlock(id)
            if (rows > 0) {
                newlyUnlockedIds.add(id)
            }
        }
    }
}
