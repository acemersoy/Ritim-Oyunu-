package com.rhythmgame.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.rhythmgame.game.HitResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VibrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gamePreferences: GamePreferences,
) {
    // Try VibratorManager (API 31+) first, then legacy service as fallback
    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = try {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            null
        }
        // Fallback to legacy service if VibratorManager returned null
        v ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } catch (_: Exception) { null }

    @Suppress("DEPRECATION")
    private val hasVibrator: Boolean = try {
        vibrator?.hasVibrator() ?: false
    } catch (_: Exception) { false }

    fun vibrateOnHit(result: HitResult) {
        if (!hasVibrator || !gamePreferences.rhythmVibrationEnabled) return
        when (result) {
            HitResult.PERFECT -> vibrate(80L, 255)
            HitResult.GREAT -> vibrate(50L, 200)
            HitResult.GOOD -> vibrate(35L, 150)
            HitResult.MISS -> return
        }
    }

    fun vibrateMenu() {
        if (!hasVibrator || !gamePreferences.menuVibrationEnabled) return
        vibrate(30L, 120)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // Last resort: try legacy API
            try {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            } catch (_: Exception) { /* device truly doesn't support vibration */ }
        }
    }
}
