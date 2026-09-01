package com.fotocontact.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.fotocontact.app.data.Feature
import com.fotocontact.app.data.Prefs
import com.fotocontact.app.data.Rule
import com.fotocontact.app.data.RuleStore
import com.fotocontact.app.overlay.CallSession
import com.fotocontact.app.overlay.MessageBuffer
import com.fotocontact.app.overlay.OverlayCoordinator
import com.fotocontact.app.util.Matcher

class WaNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notif = sbn ?: return
        val ctx = applicationContext
        if (!Prefs.isEnabled(ctx)) return
        if (!isWhatsApp(notif.packageName)) return

        val n = notif.notification ?: return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()

        val hasAnswer = n.actions?.any { isAnswer(it.title?.toString()) } == true
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val looksLikeCall = n.category == Notification.CATEGORY_CALL || hasAnswer

        val junk = Prefs.missedKeywords(ctx)
        if (Matcher.containsAny("$title $rawText", junk)) return

        if (looksLikeCall && (hasAnswer || n.fullScreenIntent != null || isOngoing)) {
            handleCall(notif, n, title, rawText)
            return
        }

        if (isOngoing) return
        handleMessage(notif, n, title, rawText)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        val current = CallSession.current
        if (current != null && current.notifKey == key) {
            OverlayCoordinator.hideCall(applicationContext)
        }
        MessageBuffer.removeByKey(key)
    }

    // ---------------- panggilan WhatsApp ----------------

    private fun handleCall(sbn: StatusBarNotification, n: Notification, title: String, text: String) {
        val ctx = applicationContext
        val isVideo = Matcher.containsAny("$title $text", Prefs.videoKeywords(ctx))
        val feature = if (isVideo) Feature.WA_VIDEO else Feature.WA_VOICE

        val rule: Rule = RuleStore.findForWhatsApp(ctx, title) ?: return
        val cfg = rule.cfgFor(feature)
        if (!cfg.enabled) return

        val answer = n.actions?.firstOrNull { isAnswer(it.title?.toString()) }?.actionIntent
        val decline = n.actions?.firstOrNull { isDecline(it.title?.toString()) }?.actionIntent

        val info = CallSession.Info(
            feature = feature,
            displayName = rule.displayName().ifBlank { title },
            subtitle = if (isVideo) "Panggilan video WhatsApp" else "Panggilan suara WhatsApp",
            photoPath = rule.photoFor(feature),
            notifKey = sbn.key,
            isSim = false,
            answer = answer,
            decline = decline
        )
        OverlayCoordinator.showCall(ctx, info, cfg)
    }

    // ---------------- pesan WhatsApp ----------------

    private fun handleMessage(sbn: StatusBarNotification, n: Notification, title: String, text: String) {
        val ctx = applicationContext

        var sender = title
        var body = text

        try {
            val style = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(n)
            val last = style?.messages?.lastOrNull()
            if (last != null) {
                val personName = last.person?.name?.toString()
                if (!personName.isNullOrBlank()) sender = personName
                val msg = last.text?.toString()
                if (!msg.isNullOrBlank()) body = msg
            }
        } catch (e: Exception) {
            // pakai extras biasa
        }

        if (body.isBlank()) return

        // format grup: "Pengirim: isi pesan"
        var rule = RuleStore.findForWhatsApp(ctx, sender)
        if (rule == null && title.isNotBlank()) rule = RuleStore.findForWhatsApp(ctx, title)
        if (rule == null && body.contains(":")) {
            val possible = body.substringBefore(":").trim()
            if (possible.length in 2..40) {
                val r2 = RuleStore.findForWhatsApp(ctx, possible)
                if (r2 != null) {
                    rule = r2
                    body = body.substringAfter(":").trim()
                }
            }
        }
        val found = rule ?: return

        val cfg = found.cfgFor(Feature.WA_MESSAGE)
        if (!cfg.enabled) return

        MessageBuffer.addOrUpdate(
            MessageBuffer.Item(
                key = sbn.key,
                name = found.displayName().ifBlank { sender },
                line = Matcher.firstLine(body),
                photoPath = found.photoFor(Feature.WA_MESSAGE),
                time = System.currentTimeMillis()
            )
        )
        OverlayCoordinator.showPeek(ctx, cfg)
    }

    private fun isWhatsApp(pkg: String?): Boolean =
        pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b"

    private fun isAnswer(t: String?): Boolean {
        val s = t?.lowercase() ?: return false
        return s.contains("answer") || s.contains("jawab") || s.contains("terima") ||
            s.contains("angkat") || s.contains("accept")
    }

    private fun isDecline(t: String?): Boolean {
        val s = t?.lowercase() ?: return false
        return s.contains("decline") || s.contains("tolak") || s.contains("reject") ||
            s.contains("abaikan") || s.contains("dismiss") || s.contains("hang")
    }
}
