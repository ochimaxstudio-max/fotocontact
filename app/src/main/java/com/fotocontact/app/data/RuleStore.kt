package com.fotocontact.app.data

import android.content.Context
import com.fotocontact.app.util.Matcher
import org.json.JSONArray
import java.io.File

object RuleStore {

    private var cache: MutableList<Rule>? = null
    private val lock = Any()

    private fun file(ctx: Context): File = File(ctx.applicationContext.filesDir, "rules.json")

    fun all(ctx: Context): MutableList<Rule> {
        synchronized(lock) {
            val existing = cache
            if (existing != null) return existing
            val list = mutableListOf<Rule>()
            try {
                val f = file(ctx)
                if (f.exists()) {
                    val arr = JSONArray(f.readText())
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i)
                        if (o != null) list.add(Rule.fromJson(o))
                    }
                }
            } catch (e: Exception) {
                // file rusak -> mulai dari kosong
            }
            cache = list
            return list
        }
    }

    fun save(ctx: Context) {
        synchronized(lock) {
            val arr = JSONArray()
            for (r in all(ctx)) arr.put(r.toJson())
            try {
                file(ctx).writeText(arr.toString())
            } catch (e: Exception) {
                // abaikan
            }
        }
    }

    fun get(ctx: Context, id: String?): Rule? {
        if (id == null) return null
        return all(ctx).firstOrNull { it.id == id }
    }

    fun upsert(ctx: Context, rule: Rule) {
        synchronized(lock) {
            val list = all(ctx)
            val idx = list.indexOfFirst { it.id == rule.id }
            if (idx >= 0) list[idx] = rule else list.add(rule)
        }
        save(ctx)
    }

    fun delete(ctx: Context, rule: Rule) {
        synchronized(lock) {
            all(ctx).removeAll { it.id == rule.id }
        }
        save(ctx)
    }

    fun findByNumber(ctx: Context, number: String?): Rule? {
        if (number.isNullOrBlank()) return null
        val key = Matcher.normalizeNumber(number)
        if (key.isEmpty()) return null
        return all(ctx).firstOrNull { r ->
            r.numbers.any { Matcher.normalizeNumber(it) == key }
        }
    }

    fun findByName(ctx: Context, name: String?): Rule? {
        if (name.isNullOrBlank()) return null
        val list = all(ctx)
        val exact = list.firstOrNull { r ->
            r.names.any { it.equals(name, ignoreCase = true) } || r.label.equals(name, ignoreCase = true)
        }
        if (exact != null) return exact
        return list.firstOrNull { r ->
            r.names.any { Matcher.looseMatch(it, name) } ||
                (r.label.isNotBlank() && Matcher.looseMatch(r.label, name))
        }
    }

    /** Cocokkan judul notifikasi WhatsApp: bisa nama, bisa nomor mentah. */
    fun findForWhatsApp(ctx: Context, title: String?): Rule? {
        val byName = findByName(ctx, title)
        if (byName != null) return byName
        if (title != null && title.count { it.isDigit() } >= 7) return findByNumber(ctx, title)
        return null
    }

    fun invalidate() {
        synchronized(lock) { cache = null }
    }
}
