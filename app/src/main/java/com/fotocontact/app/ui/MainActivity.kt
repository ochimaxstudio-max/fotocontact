package com.fotocontact.app.ui

import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.content.ComponentName
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.fotocontact.app.R
import com.fotocontact.app.data.Prefs
import com.fotocontact.app.data.RuleStore
import com.fotocontact.app.service.WaNotificationListener
import com.fotocontact.app.util.Perms

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: RuleAdapter
    private lateinit var status: TextView
    private lateinit var empty: TextView
    private lateinit var master: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        status = findViewById(R.id.status)
        empty = findViewById(R.id.empty)
        master = findViewById(R.id.master)

        master.isChecked = Prefs.isEnabled(this)
        master.setOnCheckedChangeListener { _, v -> Prefs.setEnabled(this, v) }

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        adapter = RuleAdapter(emptyList()) { rule ->
            val i = Intent(this, EditRuleActivity::class.java)
            i.putExtra(EditRuleActivity.EXTRA_ID, rule.id)
            startActivity(i)
        }
        list.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, EditRuleActivity::class.java))
        }

        findViewById<View>(R.id.btnFix).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        RuleStore.invalidate()
        val rules = RuleStore.all(this)
        adapter.submit(rules)
        empty.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
        master.isChecked = Prefs.isEnabled(this)
        updateStatus()
        requestRebind()
    }

    private fun requestRebind() {
        try {
            NotificationListenerService.requestRebind(
                ComponentName(this, WaNotificationListener::class.java)
            )
        } catch (e: Exception) {
            // abaikan
        }
    }

    private fun updateStatus() {
        val missing = mutableListOf<String>()
        if (!Perms.canOverlay(this)) missing.add("tampil di atas aplikasi lain")
        if (!Perms.notificationAccess(this)) missing.add("akses notifikasi")
        if (!Perms.phoneOk(this)) missing.add("izin telepon")
        if (!Perms.postNotifOk(this)) missing.add("izin notifikasi")

        if (missing.isEmpty()) {
            status.text = "Semua izin utama sudah aktif."
        } else {
            status.text = "Belum aktif: " + missing.joinToString(", ") + "."
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_setup -> {
                startActivity(Intent(this, SetupActivity::class.java)); true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
