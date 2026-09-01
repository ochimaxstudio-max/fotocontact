package com.fotocontact.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fotocontact.app.R
import com.fotocontact.app.util.Photos

class CropActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URI = "uri"
        const val EXTRA_ASPECT = "aspect"
        const val EXTRA_NAME = "name"
        const val EXTRA_PATH = "path"
    }

    private lateinit var cropView: CropView
    private var aspect = 9f / 16f
    private var name = "photo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        cropView = findViewById(R.id.cropView)
        aspect = intent.getFloatExtra(EXTRA_ASPECT, 9f / 16f)
        name = intent.getStringExtra(EXTRA_NAME) ?: "photo"
        cropView.setAspect(aspect)

        val hint = findViewById<TextView>(R.id.hint)
        hint.text = "Geser dan cubit untuk mengatur posisi foto"

        findViewById<View>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<View>(R.id.btnRotate).setOnClickListener { cropView.rotate90() }
        findViewById<View>(R.id.btnDone).setOnClickListener { done() }

        val uriString = intent.getStringExtra(EXTRA_URI)
        if (uriString == null) {
            finish()
            return
        }
        loadAsync(Uri.parse(uriString))
    }

    private fun loadAsync(uri: Uri) {
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val bmp = Photos.decode(this, uri)
            handler.post {
                if (bmp == null) {
                    Toast.makeText(this, "Gagal membuka gambar", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    cropView.setBitmap(bmp)
                }
            }
        }.start()
    }

    private fun done() {
        val outWidth = if (aspect < 1f) 1080 else 720
        val bmp = cropView.crop(outWidth)
        if (bmp == null) {
            Toast.makeText(this, "Gagal memotong gambar", Toast.LENGTH_SHORT).show()
            return
        }
        val path = Photos.save(this, bmp, name)
        if (path == null) {
            Toast.makeText(this, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            return
        }
        val data = Intent()
        data.putExtra(EXTRA_PATH, path)
        setResult(Activity.RESULT_OK, data)
        finish()
    }
}
