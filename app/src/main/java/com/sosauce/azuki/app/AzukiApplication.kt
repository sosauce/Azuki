package com.sosauce.azuki.app

import android.app.Application
import com.sosauce.azuki.core.di.dataModule
import com.sosauce.azuki.core.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AzukiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AzukiApplication)
            modules(dataModule, presentationModule)
        }
    }
}
