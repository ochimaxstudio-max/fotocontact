package com.fotocontact.app.overlay

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.fotocontact.app.R
import com.fotocontact.app.data.Feature
import com.fotocontact.app.util.Photos

object CallUi {

    fun bind(
        root: View,
        info: CallSession.Info,
        onAnswer: (() -> Unit)?,
        onDecline: (() -> Unit)?,
        onClose: () -> Unit
    ) {
        val photo = root.findViewById<ImageView>(R.id.photo)
        val name = root.findViewById<TextView>(R.id.name)
        val subtitle = root.findViewById<TextView>(R.id.subtitle)
        val btnAnswer = root.findViewById<View>(R.id.btnAnswer)
        val btnDecline = root.findViewById<View>(R.id.btnDecline)
        val btnClose = root.findViewById<View>(R.id.btnClose)

        val bmp = Photos.load(info.photoPath)
        if (bmp != null) photo.setImageBitmap(bmp) else photo.setImageResource(R.drawable.bg_placeholder)

        name.text = if (info.displayName.isBlank()) "Panggilan masuk" else info.displayName
        subtitle.text = info.subtitle.ifBlank { defaultSubtitle(info.feature) }

        if (onAnswer == null) {
            btnAnswer.visibility = View.GONE
        } else {
            btnAnswer.visibility = View.VISIBLE
            btnAnswer.setOnClickListener { onAnswer() }
        }

        if (onDecline == null) {
            btnDecline.visibility = View.GONE
        } else {
            btnDecline.visibility = View.VISIBLE
            btnDecline.setOnClickListener { onDecline() }
        }

        btnClose.setOnClickListener { onClose() }
    }

    fun defaultSubtitle(f: Feature): String = when (f) {
        Feature.SIM_CALL -> "Panggilan masuk"
        Feature.WA_VOICE -> "Panggilan suara WhatsApp"
        Feature.WA_VIDEO -> "Panggilan video WhatsApp"
        Feature.WA_MESSAGE -> "Pesan WhatsApp"
    }
}
