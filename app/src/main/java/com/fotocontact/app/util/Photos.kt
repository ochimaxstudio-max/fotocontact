package com.fotocontact.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object Photos {

    private fun dir(ctx: Context): File {
        val d = File(ctx.applicationContext.filesDir, "photos")
        if (!d.exists()) d.mkdirs()
        return d
    }

    fun decode(ctx: Context, uri: Uri, maxSize: Int = 2048): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            while (w / sample > maxSize || h / sample > maxSize) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bmp = ctx.contentResolver.openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val rotation = readRotation(ctx, uri)
            if (rotation != 0f) {
                val m = Matrix()
                m.postRotate(rotation)
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                if (rotated != bmp) {
                    bmp.recycle()
                    bmp = rotated
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun readRotation(ctx: Context, uri: Uri): Float {
        return try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun save(ctx: Context, bmp: Bitmap, name: String): String? {
        return try {
            val f = File(dir(ctx), "$name.jpg")
            FileOutputStream(f).use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun load(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val f = File(path)
            if (!f.exists()) null else BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            File(path).delete()
        } catch (e: Exception) {
            // abaikan
        }
    }

    /** Potong tengah jadi lingkaran, untuk avatar di daftar pesan. */
    fun circle(src: Bitmap, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val scale = size.toFloat() / minOf(src.width, src.height)
        val m = Matrix()
        m.setScale(scale, scale)
        // pusatkan secara horizontal, sedikit ke atas secara vertikal (biasanya wajah)
        val dx = (size - src.width * scale) / 2f
        val dy = if (src.height > src.width) (size - src.height * scale) * 0.25f
        else (size - src.height * scale) / 2f
        m.postTranslate(dx, dy)
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(m)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = shader
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }
}
