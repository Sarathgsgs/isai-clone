package com.isai.player

import android.app.Application
import android.os.StrictMode
import com.isai.player.ui.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class IsaiApp : Application() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()

        widgetUpdater.start()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }
}

