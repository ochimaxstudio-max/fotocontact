package com.fotocontact.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fotocontact.app.R

/**
 * Menampilkan tampilan FotoContact sebagai jendela overlay (di atas aplikasi lain).
 * Dipakai ketika layar sedang TIDAK terkunci - misalnya menutupi layar panggilan WhatsApp.
 */
object WindowOverlay {

    private val main = Handler(Looper.getMainLooper())
    private var callView: View? = null
    private var peekView: View? = null

    private fun wm(ctx: Context): WindowManager =
        ctx.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private fun overlayType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

    fun showCall(ctx: Context, info: CallSession.Info) {
        val app = ctx.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        main.post {
            hideCallInternal(app)
            try {
                val view = LayoutInflater.from(app).inflate(R.layout.overlay_call, null)
                CallUi.bind(
                    view, info,
                    onAnswer = if (info.answer != null || info.isSim) {
                        { CallActions.answer(app) }
                    } else null,
                    onDecline = if (info.decline != null || info.isSim) {
                        { CallActions.decline(app) }
                    } else null,
                    onClose = { OverlayCoordinator.hideCall(app) }
                )
                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
                )
                lp.gravity = Gravity.TOP or Gravity.START
                wm(app).addView(view, lp)
                callView = view
            } catch (e: Exception) {
                callView = null
            }
        }
    }

    fun hideCall(ctx: Context) {
        val app = ctx.applicationContext
        main.post { hideCallInternal(app) }
    }

    private fun hideCallInternal(app: Context) {
        val v = callView ?: return
        try {
            wm(app).removeView(v)
        } catch (e: Exception) {
            // abaikan
        }
        callView = null
    }

    fun showPeek(ctx: Context) {
        val app = ctx.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        main.post {
            val existing = peekView
            if (existing != null) {
                refreshPeekList(existing)
                return@post
            }
            try {
                val view = LayoutInflater.from(app).inflate(R.layout.overlay_peek, null)
                val list = view.findViewById<RecyclerView>(R.id.list)
                list.layoutManager = LinearLayoutManager(app)
                list.adapter = PeekAdapter(MessageBuffer.snapshot())
                view.findViewById<View>(R.id.btnClosePeek).setOnClickListener {
                    OverlayCoordinator.hidePeek(app)
                }
                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayType(),
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                )
                lp.gravity = Gravity.TOP
                wm(app).addView(view, lp)
                peekView = view
            } catch (e: Exception) {
                peekView = null
            }
        }
    }

    private fun refreshPeekList(view: View) {
        val list = view.findViewById<RecyclerView>(R.id.list)
        list.adapter = PeekAdapter(MessageBuffer.snapshot())
    }

    fun hidePeek(ctx: Context) {
        val app = ctx.applicationContext
        main.post {
            val v = peekView ?: return@post
            try {
                wm(app).removeView(v)
            } catch (e: Exception) {
                // abaikan
            }
            peekView = null
        }
    }
}
