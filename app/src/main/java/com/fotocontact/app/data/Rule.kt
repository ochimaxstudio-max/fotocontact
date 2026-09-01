package com.fotocontact.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FeatureCfg {
    var enabled: Boolean = true
    var tone: String? = null
    var toneName: String? = null
    var vibrate: Boolean = true

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("enabled", enabled)
        o.put("tone", tone ?: JSONObject.NULL)
        o.put("toneName", toneName ?: JSONObject.NULL)
        o.put("vibrate", vibrate)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): FeatureCfg {
            val c = FeatureCfg()
            c.enabled = o.optBoolean("enabled", true)
            c.tone = o.optString("tone", "").ifBlank { null }
            c.toneName = o.optString("toneName", "").ifBlank { null }
            c.vibrate = o.optBoolean("vibrate", true)
            return c
        }
    }
}

class Rule {
    var id: String = UUID.randomUUID().toString()

    /** Nama alias yang ditampilkan di layar. */
    var label: String = ""

    /** Nama-nama untuk mencocokkan notifikasi WhatsApp. */
    var names: MutableList<String> = mutableListOf()

    /** Nomor telepon untuk mencocokkan panggilan SIM. */
    var numbers: MutableList<String> = mutableListOf()

    var callPhoto: String? = null
    var msgPhoto: String? = null

    val cfg: MutableMap<String, FeatureCfg> = mutableMapOf()

    fun cfgFor(f: Feature): FeatureCfg {
        var c = cfg[f.key]
        if (c == null) {
            c = FeatureCfg()
            cfg[f.key] = c
        }
        return c
    }

    fun photoFor(f: Feature): String? =
        if (f == Feature.WA_MESSAGE) (msgPhoto ?: callPhoto) else (callPhoto ?: msgPhoto)

    fun displayName(): String = if (label.isNotBlank()) label else (names.firstOrNull() ?: "")

    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("label", label)
        o.put("names", JSONArray(names as List<String>))
        o.put("numbers", JSONArray(numbers as List<String>))
        o.put("callPhoto", callPhoto ?: JSONObject.NULL)
        o.put("msgPhoto", msgPhoto ?: JSONObject.NULL)
        val c = JSONObject()
        for (entry in cfg) c.put(entry.key, entry.value.toJson())
        o.put("cfg", c)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Rule {
            val r = Rule()
            r.id = o.optString("id", UUID.randomUUID().toString())
            r.label = o.optString("label", "")
            r.names = jsonToList(o.optJSONArray("names"))
            r.numbers = jsonToList(o.optJSONArray("numbers"))
            r.callPhoto = o.optString("callPhoto", "").ifBlank { null }
            r.msgPhoto = o.optString("msgPhoto", "").ifBlank { null }
            val c = o.optJSONObject("cfg")
            if (c != null) {
                val keys = c.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val obj = c.optJSONObject(k)
                    if (obj != null) r.cfg[k] = FeatureCfg.fromJson(obj)
                }
            }
            return r
        }

        private fun jsonToList(a: JSONArray?): MutableList<String> {
            val out = mutableListOf<String>()
            if (a == null) return out
            for (i in 0 until a.length()) {
                val s = a.optString(i, "").trim()
                if (s.isNotEmpty()) out.add(s)
            }
            return out
        }
    }
}
