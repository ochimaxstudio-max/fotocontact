package com.fotocontact.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        val call = NotificationChannel(
            CH_CALL, "Panggilan masuk", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Digunakan untuk menampilkan layar foto saat ada panggilan"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val peek = NotificationChannel(
            CH_PEEK, "Intip pesan", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Digunakan untuk menampilkan intip pesan WhatsApp"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannel(call)
        nm.createNotificationChannel(peek)
    }

    companion object {
        const val CH_CALL = "fc_call"
        const val CH_PEEK = "fc_peek"
    }
}
