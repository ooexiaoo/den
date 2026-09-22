package com.den.app

import android.app.Application

class DenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        try {
            AppGraph.container = AppContainer(this)
        } catch (t: Throwable) {
            // Capture startup failures instead of crash-looping so the user can read them.
            CrashReporter.capture(this, Thread.currentThread(), t)
        }
    }
}