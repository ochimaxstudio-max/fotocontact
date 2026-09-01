package com.fotocontact.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fotocontact.app.R

class PeekActivity : AppCompatActivity() {

    private lateinit var list: RecyclerView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                OverlayCoordinator.ACTION_CLOSE_PEEK -> finishAndRemoveTask()
                OverlayCoordinator.ACTION_REFRESH_PEEK -> refresh()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverKeyguard()
        setContentView(R.layout.overlay_peek_activity)

        list = findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        refresh()

        findViewById<View>(R.id.btnClosePeek).setOnClickListener {
            OverlayCoordinator.hidePeek(this)
        }
        findViewById<View>(R.id.root).setOnClickListener {
            OverlayCoordinator.hidePeek(this)
        }

        val filter = IntentFilter()
        filter.addAction(OverlayCoordinator.ACTION_CLOSE_PEEK)
        filter.addAction(OverlayCoordinator.ACTION_REFRESH_PEEK)
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val items = MessageBuffer.snapshot()
        if (items.isEmpty()) {
            finish()
            return
        }
        list.adapter = PeekAdapter(items)
    }

    private fun showOverKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        OverlayCoordinator.hidePeek(this)
        super.onBackPressed()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // abaikan
        }
        super.onDestroy()
    }
}
