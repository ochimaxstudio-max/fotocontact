package com.fotocontact.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/** Tampilan potong foto sederhana: geser dengan satu jari, cubit untuk zoom. */
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private val imgMatrix = Matrix()
    private val frame = RectF()
    private var aspect = 9f / 16f
    private var minScale = 1f

    private val imgPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint().apply { color = Color.parseColor("#B3000000") }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val current = currentScale()
                var factor = detector.scaleFactor
                val target = current * factor
                val max = minScale * 8f
                if (target < minScale) factor = minScale / current
                if (target > max) factor = max / current
                imgMatrix.postScale(factor, factor, detector.focusX, detector.focusY)
                clamp()
                invalidate()
                return true
            }
        }
    )

    fun setAspect(a: Float) {
        aspect = if (a <= 0f) 1f else a
        computeFrame()
        reset()
        invalidate()
    }

    fun setBitmap(b: Bitmap?) {
        bitmap = b
        computeFrame()
        reset()
        invalidate()
    }

    fun rotate90() {
        val b = bitmap ?: return
        val m = Matrix()
        m.postRotate(90f)
        val out = Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        bitmap = out
        reset()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeFrame()
        reset()
    }

    private fun computeFrame() {
        if (width == 0 || height == 0) return
        val pad = 24f
        val availW = width - pad * 2
        val availH = height - pad * 2
        var fw = availW
        var fh = fw / aspect
        if (fh > availH) {
            fh = availH
            fw = fh * aspect
        }
        val left = (width - fw) / 2f
        val top = (height - fh) / 2f
        frame.set(left, top, left + fw, top + fh)
    }

    private fun reset() {
        val b = bitmap ?: return
        if (frame.width() <= 0f) return
        imgMatrix.reset()
        val s = maxOf(frame.width() / b.width, frame.height() / b.height)
        minScale = s
        imgMatrix.postScale(s, s)
        val dx = frame.centerX() - b.width * s / 2f
        val dy = frame.centerY() - b.height * s / 2f
        imgMatrix.postTranslate(dx, dy)
    }

    private fun currentScale(): Float {
        val v = FloatArray(9)
        imgMatrix.getValues(v)
        return v[Matrix.MSCALE_X]
    }

    private fun clamp() {
        val b = bitmap ?: return
        val r = RectF(0f, 0f, b.width.toFloat(), b.height.toFloat())
        imgMatrix.mapRect(r)
        var dx = 0f
        var dy = 0f
        if (r.left > frame.left) dx = frame.left - r.left
        if (r.right < frame.right) dx = frame.right - r.right
        if (r.top > frame.top) dy = frame.top - r.top
        if (r.bottom < frame.bottom) dy = frame.bottom - r.bottom
        imgMatrix.postTranslate(dx, dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && event.pointerCount == 1 && !scaleDetector.isInProgress) {
                    imgMatrix.postTranslate(event.x - lastX, event.y - lastY)
                    lastX = event.x
                    lastY = event.y
                    clamp()
                    invalidate()
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                lastX = event.x
                lastY = event.y
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = bitmap ?: return
        canvas.drawBitmap(b, imgMatrix, imgPaint)

        canvas.drawRect(0f, 0f, width.toFloat(), frame.top, dimPaint)
        canvas.drawRect(0f, frame.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, frame.top, frame.left, frame.bottom, dimPaint)
        canvas.drawRect(frame.right, frame.top, width.toFloat(), frame.bottom, dimPaint)
        canvas.drawRect(frame, borderPaint)
    }

    fun crop(outWidth: Int): Bitmap? {
        val b = bitmap ?: return null
        if (frame.width() <= 0f) return null
        val outHeight = (outWidth / aspect).toInt().coerceAtLeast(1)
        return try {
            val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val m = Matrix(imgMatrix)
            m.postTranslate(-frame.left, -frame.top)
            val s = outWidth / frame.width()
            m.postScale(s, s)
            c.drawBitmap(b, m, imgPaint)
            out
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
