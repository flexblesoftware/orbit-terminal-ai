package com.orbitterminal.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class OrbitApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Plant Timber debug logging
        Timber.plant(Timber.DebugTree())

        // Global uncaught exception handler to safely log unexpected crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("OrbitApp", "FATAL UNCAUGHT EXCEPTION in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
