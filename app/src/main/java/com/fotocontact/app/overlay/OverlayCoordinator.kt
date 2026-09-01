package com.fotocontact.app.overlay

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fotocontact.app.App
import com.fotocontact.app.R
import com.fotocontact.app.data.FeatureCfg
import com.fotocontact.app.data.Prefs
import com.fotocontact.app.util.RingtonePlayer

object OverlayCoordinator {

    const val ACTION_CLOSE_CALL = "com.fotocontact.app.CLOSE_CALL"
    const val ACTION_CLOSE_PEEK = "com.fotocontact.app.CLOSE_PEEK"
    const val ACTION_REFRESH_PEEK = "com.fotocontact.app.REFRESH_PEEK"

    private const val NOTIF_CALL = 1001
    private const val NOTIF_PEEK = 1002

    private val main = Handler(Looper.getMainLooper())
    private var peekTimeout: Runnable? = null

    fun isLockedOrScreenOff(ctx: Context): Boolean {
        return try {
            val km = ctx.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            km.isKeyguardLocked || !pm.isInteractive
        } catch (e: Exception) {
            false
        }
    }

    // ---------------- PANGGILAN ----------------

    fun showCall(ctx: Context, info: CallSession.Info, cfg: FeatureCfg?) {
        val app = ctx.applicationContext
        CallSession.current = info
        if (cfg != null) RingtonePlayer.play(app, cfg, true)

        if (Prefs.forceFullScreen(app) || isLockedOrScreenOff(app)) {
            startCallActivity(app)
            postFullScreenFallback(app, info)
        } else {
            WindowOverlay.showCall(app, info)
        }
    }

    private fun startCallActivity(app: Context) {
        try {
            val i = Intent(app, CallOverlayActivity::class.java)
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            app.startActivity(i)
            // sebagian ponsel menampilkan layar panggilan bawaan sesaat setelah kita,
            // jadi kita naikkan lagi tampilan FotoContact beberapa saat kemudian.
            main.postDelayed({ if (CallSession.current != null) safeStart(app) }, 700)
            main.postDelayed({ if (CallSession.current != null) safeStart(app) }, 1800)
        } catch (e: Exception) {
            // ditangani lewat notifikasi layar penuh
        }
    }

    private fun safeStart(app: Context) {
        try {
            val i = Intent(app, CallOverlayActivity::class.java)
            i.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            app.startActivity(i)
        } catch (e: Exception) {
            // abaikan
        }
    }

    private fun postFullScreenFallback(app: Context, info: CallSession.Info) {
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(app, CallOverlayActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getActivity(app, 10, intent, flags)
            val n = NotificationCompat.Builder(app, App.CH_CALL)
                .setSmallIcon(R.drawable.ic_stat_call)
                .setContentTitle(info.displayName.ifBlank { "Panggilan masuk" })
                .setContentText(info.subtitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setSilent(true)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_CALL, n)
        } catch (e: Exception) {
            // abaikan
        }
    }

    fun hideCall(ctx: Context) {
        val app = ctx.applicationContext
        CallSession.current = null
        RingtonePlayer.stop(app)
        WindowOverlay.hideCall(app)
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_CALL)
        } catch (e: Exception) {
            // abaikan
        }
        broadcast(app, ACTION_CLOSE_CALL)
    }

    // ---------------- INTIP PESAN ----------------

    fun showPeek(ctx: Context, cfg: FeatureCfg?, force: Boolean = false) {
        val app = ctx.applicationContext
        val locked = isLockedOrScreenOff(app)
        if (Prefs.peekOnlyWhenLocked(app) && !locked && !force) return

        if (cfg != null) RingtonePlayer.play(app, cfg, false)

        if (locked || Prefs.forceFullScreen(app)) {
            try {
                val i = Intent(app, PeekActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                app.startActivity(i)
            } catch (e: Exception) {
                // abaikan
            }
            broadcast(app, ACTION_REFRESH_PEEK)
            postPeekFallback(app)
        } else {
            WindowOverlay.showPeek(app)
        }

        val delay = Prefs.peekSeconds(app).coerceIn(3, 60) * 1000L
        peekTimeout?.let { main.removeCallbacks(it) }
        val r = Runnable { hidePeek(app) }
        peekTimeout = r
        main.postDelayed(r, delay)
    }

    private fun postPeekFallback(app: Context) {
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val intent = Intent(app, PeekActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pi = PendingIntent.getActivity(app, 11, intent, flags)
            val n = NotificationCompat.Builder(app, App.CH_PEEK)
                .setSmallIcon(R.drawable.ic_stat_msg)
                .setContentTitle("FotoContact")
                .setContentText("Ketuk untuk mengintip pesan")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .setAutoCancel(true)
                .setFullScreenIntent(pi, true)
                .setContentIntent(pi)
                .build()
            nm.notify(NOTIF_PEEK, n)
        } catch (e: Exception) {
            // abaikan
        }
    }

    fun hidePeek(ctx: Context) {
        val app = ctx.applicationContext
        peekTimeout?.let { main.removeCallbacks(it) }
        peekTimeout = null
        RingtonePlayer.stop(app)
        WindowOverlay.hidePeek(app)
        MessageBuffer.clear()
        try {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_PEEK)
        } catch (e: Exception) {
            // abaikan
        }
        broadcast(app, ACTION_CLOSE_PEEK)
    }

    private fun broadcast(app: Context, action: String) {
        try {
            val i = Intent(action)
            i.setPackage(app.packageName)
            app.sendBroadcast(i)
        } catch (e: Exception) {
            // abaikan
        }
    }
}
