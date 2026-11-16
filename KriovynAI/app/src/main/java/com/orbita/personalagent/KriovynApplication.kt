package com.orbita.personalagent

import android.app.Application
import com.orbita.personalagent.di.AppContainer
import com.orbita.personalagent.worker.NotificationHelper

class KriovynApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
    }
}
