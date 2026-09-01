package com.fotocontact.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fotocontact.app.R

class CallOverlayActivity : AppCompatActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            finishAndRemoveTask()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverKeyguard()
        setContentView(R.layout.overlay_call)
        render()

        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(OverlayCoordinator.ACTION_CLOSE_CALL),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val info = CallSession.current
        if (info == null) {
            finish()
            return
        }
        CallUi.bind(
            findViewById(R.id.root),
            info,
            onAnswer = if (info.answer != null || info.isSim) {
                { CallActions.answer(this) }
            } else null,
            onDecline = if (info.decline != null || info.isSim) {
                { CallActions.decline(this) }
            } else null,
            onClose = { OverlayCoordinator.hideCall(this) }
        )
    }

    private fun showOverKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // biarkan pengguna menutup tampilan dengan tombol kembali
        OverlayCoordinator.hideCall(this)
        super.onBackPressed()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            // abaikan
        }
        super.onDestroy()
    }
}
