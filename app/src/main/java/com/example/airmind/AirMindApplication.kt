package com.example.airmind

import android.app.Application
import com.example.airmind.core.di.AppContainer
import com.example.airmind.core.di.DefaultAppContainer

class AirMindApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}
