package com.fotocontact.app.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object Perms {

    fun canOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun notificationAccess(ctx: Context): Boolean {
        return try {
            val flat = Settings.Secure.getString(
                ctx.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            flat.contains(ctx.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun granted(ctx: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    fun phoneOk(ctx: Context): Boolean =
        granted(ctx, Manifest.permission.READ_PHONE_STATE) &&
            granted(ctx, Manifest.permission.READ_CALL_LOG)

    fun contactsOk(ctx: Context): Boolean = granted(ctx, Manifest.permission.READ_CONTACTS)

    fun answerOk(ctx: Context): Boolean = granted(ctx, Manifest.permission.ANSWER_PHONE_CALLS)

    fun postNotifOk(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || granted(ctx, "android.permission.POST_NOTIFICATIONS")

    fun batteryUnrestricted(ctx: Context): Boolean {
        return try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(ctx.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun dndAccess(ctx: Context): Boolean {
        return try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.isNotificationPolicyAccessGranted
        } catch (e: Exception) {
            false
        }
    }

    fun fullScreenIntentOk(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.canUseFullScreenIntent()
        } catch (e: Exception) {
            true
        }
    }

    fun runtimePermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        if (Build.VERSION.SDK_INT >= 33) {
            list.add("android.permission.POST_NOTIFICATIONS")
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return list.toTypedArray()
    }
}
