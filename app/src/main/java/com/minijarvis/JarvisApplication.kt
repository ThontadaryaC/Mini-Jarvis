package com.minijarvis

import android.app.Application
import com.minijarvis.core.JarvisCore

class JarvisApplication : Application() {

    lateinit var jarvisCore: JarvisCore
        private set

    companion object {
        lateinit var instance: JarvisApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        jarvisCore = JarvisCore(applicationContext)
    }
}
