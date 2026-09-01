package com.fotocontact.app.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import com.fotocontact.app.R
import com.fotocontact.app.data.Feature
import com.fotocontact.app.data.Rule
import com.fotocontact.app.data.RuleStore
import com.fotocontact.app.overlay.CallSession
import com.fotocontact.app.overlay.MessageBuffer
import com.fotocontact.app.overlay.OverlayCoordinator
import com.fotocontact.app.util.Photos
import java.util.UUID

class EditRuleActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "rule_id"
        private const val SLOT_CALL = 1
        private const val SLOT_MSG = 2
    }

    private lateinit var rule: Rule
    private var slot = SLOT_CALL
    private var toneFeature: Feature? = null

    private lateinit var etAlias: EditText
    private lateinit var etNames: EditText
    private lateinit var etNumbers: EditText
    private lateinit var imgCall: ImageView
    private lateinit var imgMsg: ImageView

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) startCrop(uri)
        }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val path = res.data?.getStringExtra(CropActivity.EXTRA_PATH) ?: return@registerForActivityResult
                if (slot == SLOT_CALL) {
                    Photos.delete(rule.callPhoto)
                    rule.callPhoto = path
                } else {
                    Photos.delete(rule.msgPhoto)
                    rule.msgPhoto = path
                }
                refreshPhotos()
            }
        }

    private val toneLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val f = toneFeature ?: return@registerForActivityResult
            val uri = res.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            val cfg = rule.cfgFor(f)
            if (uri == null) {
                cfg.tone = null
                cfg.toneName = null
            } else {
                cfg.tone = uri.toString()
                cfg.toneName = try {
                    RingtoneManager.getRingtone(this, uri)?.getTitle(this)
                } catch (e: Exception) {
                    "Nada khusus"
                }
            }
            bindFeatures()
        }

    private val contactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val uri = res.data?.data ?: return@registerForActivityResult
            readContact(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_rule)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val id = intent.getStringExtra(EXTRA_ID)
        rule = RuleStore.get(this, id) ?: Rule()

        etAlias = findViewById(R.id.etAlias)
        etNames = findViewById(R.id.etNames)
        etNumbers = findViewById(R.id.etNumbers)
        imgCall = findViewById(R.id.imgCall)
        imgMsg = findViewById(R.id.imgMsg)

        etAlias.setText(rule.label)
        etNames.setText(rule.names.joinToString(", "))
        etNumbers.setText(rule.numbers.joinToString(", "))

        findViewById<View>(R.id.btnPickContact).setOnClickListener {
            val i = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            try {
                contactLauncher.launch(i)
            } catch (e: Exception) {
                toast("Tidak ada aplikasi kontak")
            }
        }

        findViewById<View>(R.id.btnCallPhoto).setOnClickListener {
            slot = SLOT_CALL
            pickImage.launch("image/*")
        }
        findViewById<View>(R.id.btnMsgPhoto).setOnClickListener {
            slot = SLOT_MSG
            pickImage.launch("image/*")
        }

        findViewById<View>(R.id.btnSave).setOnClickListener { save() }
        findViewById<View>(R.id.btnDelete).setOnClickListener { confirmDelete() }
        findViewById<View>(R.id.btnTestCall).setOnClickListener { testCall() }
        findViewById<View>(R.id.btnTestPeek).setOnClickListener { testPeek() }

        bindFeatures()
        refreshPhotos()
    }

    private fun blockOf(f: Feature): View = when (f) {
        Feature.SIM_CALL -> findViewById(R.id.blockSim)
        Feature.WA_VOICE -> findViewById(R.id.blockVoice)
        Feature.WA_VIDEO -> findViewById(R.id.blockVideo)
        Feature.WA_MESSAGE -> findViewById(R.id.blockMessage)
    }

    private fun bindFeatures() {
        for (f in Feature.values()) {
            val block = blockOf(f)
            val cfg = rule.cfgFor(f)

            val sw = block.findViewById<SwitchMaterial>(R.id.featureSwitch)
            sw.text = f.label
            sw.setOnCheckedChangeListener(null)
            sw.isChecked = cfg.enabled
            sw.setOnCheckedChangeListener { _, v -> cfg.enabled = v }

            val toneName = block.findViewById<TextView>(R.id.toneName)
            toneName.text = cfg.toneName ?: "Nada bawaan sistem (tidak diganti)"

            block.findViewById<View>(R.id.btnTone).setOnClickListener { pickTone(f) }

            val vib = block.findViewById<MaterialCheckBox>(R.id.cbVibrate)
            vib.setOnCheckedChangeListener(null)
            vib.isChecked = cfg.vibrate
            vib.setOnCheckedChangeListener { _, v -> cfg.vibrate = v }
        }
    }

    private fun pickTone(f: Feature) {
        toneFeature = f
        val cfg = rule.cfgFor(f)
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih nada untuk " + f.label)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
        val current = cfg.tone
        if (!current.isNullOrBlank()) {
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current))
        }
        try {
            toneLauncher.launch(i)
        } catch (e: Exception) {
            toast("Pemilih nada tidak tersedia")
        }
    }

    private fun startCrop(uri: Uri) {
        val i = Intent(this, CropActivity::class.java)
        i.putExtra(CropActivity.EXTRA_URI, uri.toString())
        i.putExtra(
            CropActivity.EXTRA_ASPECT,
            if (slot == SLOT_CALL) 9f / 16f else 1f
        )
        i.putExtra(CropActivity.EXTRA_NAME, rule.id + "_" + (if (slot == SLOT_CALL) "call" else "msg") + "_" + UUID.randomUUID().toString().take(6))
        cropLauncher.launch(i)
    }

    private fun refreshPhotos() {
        val c = Photos.load(rule.callPhoto)
        if (c != null) imgCall.setImageBitmap(c) else imgCall.setImageResource(R.drawable.bg_placeholder)
        val m = Photos.load(rule.msgPhoto)
        if (m != null) imgMsg.setImageBitmap(Photos.circle(m, 300)) else imgMsg.setImageResource(R.drawable.bg_placeholder)
    }

    private fun readContact(uri: Uri) {
        try {
            val proj = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            contentResolver.query(uri, proj, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0) ?: ""
                    val number = c.getString(1) ?: ""
                    if (etAlias.text.isNullOrBlank()) etAlias.setText(name)
                    val names = splitList(etNames.text.toString()).toMutableList()
                    if (name.isNotBlank() && !names.any { it.equals(name, true) }) names.add(name)
                    etNames.setText(names.joinToString(", "))
                    val numbers = splitList(etNumbers.text.toString()).toMutableList()
                    if (number.isNotBlank() && !numbers.contains(number)) numbers.add(number)
                    etNumbers.setText(numbers.joinToString(", "))
                }
            }
        } catch (e: Exception) {
            toast("Gagal membaca kontak (cek izin Kontak)")
        }
    }

    private fun splitList(s: String): List<String> =
        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun collect() {
        rule.label = etAlias.text.toString().trim()
        rule.names = splitList(etNames.text.toString()).toMutableList()
        rule.numbers = splitList(etNumbers.text.toString()).toMutableList()
        if (rule.names.isEmpty() && rule.label.isNotBlank()) rule.names.add(rule.label)
    }

    private fun save() {
        collect()
        if (rule.label.isBlank() && rule.names.isEmpty() && rule.numbers.isEmpty()) {
            toast("Isi minimal nama alias atau nomor")
            return
        }
        RuleStore.upsert(this, rule)
        toast("Tersimpan")
        finish()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Hapus kontak ini?")
            .setMessage("Pengaturan foto dan nada untuk kontak ini akan dihapus.")
            .setPositiveButton("Hapus") { _, _ ->
                Photos.delete(rule.callPhoto)
                Photos.delete(rule.msgPhoto)
                RuleStore.delete(this, rule)
                finish()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun testCall() {
        collect()
        val info = CallSession.Info(
            feature = Feature.WA_VOICE,
            displayName = rule.displayName().ifBlank { "Contoh Kontak" },
            subtitle = "Contoh tampilan panggilan",
            photoPath = rule.callPhoto ?: rule.msgPhoto,
            notifKey = null,
            isSim = false,
            answer = null,
            decline = null,
            preview = true
        )
        OverlayCoordinator.showCall(this, info, rule.cfgFor(Feature.WA_VOICE))
    }

    private fun testPeek() {
        collect()
        MessageBuffer.addOrUpdate(
            MessageBuffer.Item(
                key = "preview",
                name = rule.displayName().ifBlank { "Contoh Kontak" },
                line = "Ini contoh baris pertama pesan WhatsApp",
                photoPath = rule.msgPhoto ?: rule.callPhoto,
                time = System.currentTimeMillis()
            )
        )
        OverlayCoordinator.showPeek(this, rule.cfgFor(Feature.WA_MESSAGE), true)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
