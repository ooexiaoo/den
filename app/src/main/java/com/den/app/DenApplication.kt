package com.den.app

import android.app.Application

class DenApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.container = AppContainer(this)
    }
}