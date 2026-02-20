package com.rhythmgame

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RhythmGameApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
