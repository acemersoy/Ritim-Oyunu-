package com.rhythmgame

import android.app.Application
import com.rhythmgame.network.ServerDiscovery
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RhythmGameApp : Application() {

    @Inject
    lateinit var serverDiscovery: ServerDiscovery

    override fun onCreate() {
        super.onCreate()
        serverDiscovery.startDiscovery()
    }
}
