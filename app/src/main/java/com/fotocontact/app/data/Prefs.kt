package com.fotocontact.app.data

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val NAME = "fotocontact"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var masterEnabledDefault = true

    fun isEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("enabled", masterEnabledDefault)
    fun setEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("enabled", v).apply()

    /** Bisukan dering/notifikasi sistem selama tampilan FotoContact aktif. */
    fun silenceSystem(ctx: Context): Boolean = sp(ctx).getBoolean("silence_system", false)
    fun setSilenceSystem(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean("silence_system", v).apply()

    /** Detik tampilan intip pesan sebelum hilang otomatis. */
    fun peekSeconds(ctx: Context): Int = sp(ctx).getInt("peek_seconds", 10)
    fun setPeekSeconds(ctx: Context, v: Int) = sp(ctx).edit().putInt("peek_seconds", v).apply()

    /** Paksa selalu pakai layar penuh (Activity), termasuk saat layar tidak terkunci. */
    fun forceFullScreen(ctx: Context): Boolean = sp(ctx).getBoolean("force_fullscreen", false)
    fun setForceFullScreen(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean("force_fullscreen", v).apply()

    /** Tampilkan intip pesan hanya ketika layar terkunci. */
    fun peekOnlyWhenLocked(ctx: Context): Boolean = sp(ctx).getBoolean("peek_locked_only", true)
    fun setPeekOnlyWhenLocked(ctx: Context, v: Boolean) =
        sp(ctx).edit().putBoolean("peek_locked_only", v).apply()

    fun videoKeywords(ctx: Context): String =
        sp(ctx).getString("kw_video", DEFAULT_VIDEO) ?: DEFAULT_VIDEO

    fun setVideoKeywords(ctx: Context, v: String) = sp(ctx).edit().putString("kw_video", v).apply()

    fun missedKeywords(ctx: Context): String =
        sp(ctx).getString("kw_missed", DEFAULT_MISSED) ?: DEFAULT_MISSED

    fun setMissedKeywords(ctx: Context, v: String) = sp(ctx).edit().putString("kw_missed", v).apply()

    const val DEFAULT_VIDEO = "video call,panggilan video,videocall,video"
    const val DEFAULT_MISSED =
        "missed,tak terjawab,tidak terjawab,terlewat,memeriksa pesan,checking for new messages,backup,cadangan"
}
