package com.fotocontact.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.fotocontact.app.R
import com.fotocontact.app.util.Perms

class SetupActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            render()
        }

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            render()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        container = findViewById(R.id.container)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        container.removeAllViews()

        row(
            "Tampil di atas aplikasi lain",
            "Wajib. Dipakai untuk menampilkan foto di atas layar panggilan.",
            Perms.canOverlay(this)
        ) {
            open(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        row(
            "Akses notifikasi",
            "Wajib untuk WhatsApp. Dipakai membaca notifikasi panggilan dan pesan WhatsApp.",
            Perms.notificationAccess(this)
        ) {
            open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        row(
            "Izin telepon, log panggilan, kontak",
            "Wajib untuk panggilan SIM, supaya nomor pemanggil bisa dikenali.",
            Perms.phoneOk(this) && Perms.contactsOk(this)
        ) {
            permLauncher.launch(Perms.runtimePermissions())
        }

        row(
            "Izin menjawab panggilan",
            "Opsional. Supaya tombol Jawab / Tolak berfungsi untuk panggilan SIM.",
            Perms.answerOk(this)
        ) {
            permLauncher.launch(Perms.runtimePermissions())
        }

        row(
            "Izin notifikasi",
            "Wajib di Android 13+. Dipakai sebagai cadangan menampilkan layar penuh.",
            Perms.postNotifOk(this)
        ) {
            permLauncher.launch(Perms.runtimePermissions())
        }

        row(
            "Bebaskan dari hemat baterai",
            "Disarankan. Supaya aplikasi tidak dimatikan sistem saat layar mati.",
            Perms.batteryUnrestricted(this)
        ) {
            try {
                open(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (e: Exception) {
                open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        row(
            "Akses Jangan Ganggu",
            "Opsional. Diperlukan jika ingin membisukan dering bawaan saat FotoContact tampil.",
            Perms.dndAccess(this)
        ) {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= 34) {
            row(
                "Notifikasi layar penuh",
                "Disarankan di Android 14+ sebagai cadangan saat layar terkunci.",
                Perms.fullScreenIntentOk(this)
            ) {
                open(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }

        note(
            "Catatan penting",
            "Agar nada khusus FotoContact tidak bertabrakan dengan nada WhatsApp, " +
                "buka chat kontak tersebut di WhatsApp lalu atur Notifikasi Khusus " +
                "menjadi senyap (nada: None). Untuk Xiaomi/Oppo/Vivo/Realme, aktifkan juga " +
                "Autostart dan izin 'Tampilkan di layar kunci' untuk FotoContact."
        )
    }

    private fun row(title: String, desc: String, ok: Boolean, action: () -> Unit) {
        val v = LayoutInflater.from(this).inflate(R.layout.item_setup, container, false)
        v.findViewById<TextView>(R.id.title).text = title
        v.findViewById<TextView>(R.id.desc).text = desc
        val st = v.findViewById<TextView>(R.id.state)
        st.text = if (ok) "AKTIF" else "BELUM"
        val btn = v.findViewById<Button>(R.id.btnOpen)
        btn.text = if (ok) "Ubah" else "Aktifkan"
        btn.setOnClickListener { action() }
        container.addView(v)
    }

    private fun note(title: String, desc: String) {
        val v = LayoutInflater.from(this).inflate(R.layout.item_setup, container, false)
        v.findViewById<TextView>(R.id.title).text = title
        v.findViewById<TextView>(R.id.desc).text = desc
        v.findViewById<TextView>(R.id.state).visibility = View.GONE
        v.findViewById<Button>(R.id.btnOpen).visibility = View.GONE
        container.addView(v)
    }

    private fun open(intent: Intent) {
        try {
            settingsLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Halaman pengaturan tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }
}
