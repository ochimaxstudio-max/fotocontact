package com.fotocontact.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.fotocontact.app.data.Feature
import com.fotocontact.app.data.Prefs
import com.fotocontact.app.data.RuleStore
import com.fotocontact.app.overlay.CallSession
import com.fotocontact.app.overlay.OverlayCoordinator

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context?.applicationContext ?: return
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!Prefs.isEnabled(ctx)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            @Suppress("DEPRECATION")
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (number.isNullOrBlank()) return
            handleRinging(ctx, number)
        } else {
            val current = CallSession.current
            if (current != null && current.isSim) OverlayCoordinator.hideCall(ctx)
        }
    }

    private fun handleRinging(ctx: Context, number: String) {
        val rule = RuleStore.findByNumber(ctx, number) ?: return
        val cfg = rule.cfgFor(Feature.SIM_CALL)
        if (!cfg.enabled) return

        val info = CallSession.Info(
            feature = Feature.SIM_CALL,
            displayName = rule.displayName().ifBlank { number },
            subtitle = "Panggilan masuk",
            photoPath = rule.photoFor(Feature.SIM_CALL),
            notifKey = null,
            isSim = true,
            answer = null,
            decline = null
        )
        OverlayCoordinator.showCall(ctx, info, cfg)
    }
}
