package com.fotocontact.app.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.fotocontact.app.R
import com.fotocontact.app.data.Prefs

class SettingsActivity : AppCompatActivity() {

    private lateinit var etVideo: EditText
    private lateinit var etMissed: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val swSilence = findViewById<SwitchMaterial>(R.id.swSilence)
        swSilence.isChecked = Prefs.silenceSystem(this)
        swSilence.setOnCheckedChangeListener { _, v -> Prefs.setSilenceSystem(this, v) }

        val swFull = findViewById<SwitchMaterial>(R.id.swFullScreen)
        swFull.isChecked = Prefs.forceFullScreen(this)
        swFull.setOnCheckedChangeListener { _, v -> Prefs.setForceFullScreen(this, v) }

        val swLocked = findViewById<SwitchMaterial>(R.id.swPeekLocked)
        swLocked.isChecked = Prefs.peekOnlyWhenLocked(this)
        swLocked.setOnCheckedChangeListener { _, v -> Prefs.setPeekOnlyWhenLocked(this, v) }

        val seek = findViewById<SeekBar>(R.id.seekPeek)
        val seekLabel = findViewById<TextView>(R.id.peekLabel)
        seek.max = 57
        seek.progress = (Prefs.peekSeconds(this) - 3).coerceIn(0, 57)
        seekLabel.text = "Lama tampil intip pesan: " + Prefs.peekSeconds(this) + " detik"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val v = progress + 3
                seekLabel.text = "Lama tampil intip pesan: $v detik"
                Prefs.setPeekSeconds(this@SettingsActivity, v)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        etVideo = findViewById(R.id.etVideoKeywords)
        etVideo.setText(Prefs.videoKeywords(this))
        etMissed = findViewById(R.id.etMissedKeywords)
        etMissed.setText(Prefs.missedKeywords(this))
    }

    override fun onPause() {
        super.onPause()
        Prefs.setVideoKeywords(this, etVideo.text.toString())
        Prefs.setMissedKeywords(this, etMissed.text.toString())
    }
}
