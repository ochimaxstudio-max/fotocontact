package com.fotocontact.app.overlay

import android.Manifest
import android.content.Context
import android.os.Build
import android.telecom.TelecomManager
import android.widget.Toast
import com.fotocontact.app.util.Perms

object CallActions {

    fun answer(ctx: Context) {
        val app = ctx.applicationContext
        val info = CallSession.current
        if (info == null) {
            OverlayCoordinator.hideCall(app)
            return
        }
        if (info.preview) {
            OverlayCoordinator.hideCall(app)
            return
        }
        if (info.isSim) {
            try {
                if (!Perms.granted(app, Manifest.permission.ANSWER_PHONE_CALLS)) {
                    Toast.makeText(app, "Izin 'Jawab panggilan' belum aktif", Toast.LENGTH_SHORT).show()
                    return
                }
                val tm = app.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                tm.acceptRingingCall()
            } catch (e: Exception) {
                Toast.makeText(app, "Tidak bisa menjawab otomatis", Toast.LENGTH_SHORT).show()
            }
        } else {
            try {
                info.answer?.send()
            } catch (e: Exception) {
                Toast.makeText(app, "Tombol jawab tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }
        OverlayCoordinator.hideCall(app)
    }

    fun decline(ctx: Context) {
        val app = ctx.applicationContext
        val info = CallSession.current
        if (info == null) {
            OverlayCoordinator.hideCall(app)
            return
        }
        if (info.preview) {
            OverlayCoordinator.hideCall(app)
            return
        }
        if (info.isSim) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    Perms.granted(app, Manifest.permission.ANSWER_PHONE_CALLS)
                ) {
                    val tm = app.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    tm.endCall()
                } else {
                    Toast.makeText(app, "Tolak panggilan tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // abaikan
            }
        } else {
            try {
                info.decline?.send()
            } catch (e: Exception) {
                // abaikan
            }
        }
        OverlayCoordinator.hideCall(app)
    }
}
