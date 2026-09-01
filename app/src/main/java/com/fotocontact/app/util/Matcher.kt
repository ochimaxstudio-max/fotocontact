package com.fotocontact.app.util

object Matcher {

    /** Ambil 9 digit terakhir supaya +62 / 0 di depan tidak jadi masalah. */
    fun normalizeNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length <= 9) digits else digits.substring(digits.length - 9)
    }

    fun looseMatch(a: String, b: String): Boolean {
        val x = simplify(a)
        val y = simplify(b)
        if (x.isEmpty() || y.isEmpty()) return false
        return x == y || x.contains(y) || y.contains(x)
    }

    private fun simplify(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    fun containsAny(text: String, csvKeywords: String): Boolean {
        val t = text.lowercase()
        for (raw in csvKeywords.split(",")) {
            val k = raw.trim().lowercase()
            if (k.isNotEmpty() && t.contains(k)) return true
        }
        return false
    }

    fun firstLine(text: String, max: Int = 90): String {
        var line = text.trim().lineSequence().firstOrNull { it.isNotBlank() } ?: ""
        line = line.trim()
        if (line.length > max) line = line.substring(0, max).trimEnd() + "…"
        return line
    }
}
