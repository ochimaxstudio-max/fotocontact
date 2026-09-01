package com.fotocontact.app.overlay

import android.app.PendingIntent
import com.fotocontact.app.data.Feature

object CallSession {

    class Info(
        val feature: Feature,
        val displayName: String,
        val subtitle: String,
        val photoPath: String?,
        val notifKey: String?,
        val isSim: Boolean,
        val answer: PendingIntent?,
        val decline: PendingIntent?,
        val preview: Boolean = false
    )

    @Volatile
    var current: Info? = null
}
