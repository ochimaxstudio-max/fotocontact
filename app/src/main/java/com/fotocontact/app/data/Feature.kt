package com.fotocontact.app.data

enum class Feature(val key: String, val label: String) {
    SIM_CALL("sim_call", "Panggilan suara SIM"),
    WA_VOICE("wa_voice", "Panggilan suara WhatsApp"),
    WA_VIDEO("wa_video", "Panggilan video WhatsApp"),
    WA_MESSAGE("wa_message", "Pesan WhatsApp");

    companion object {
        fun from(key: String?): Feature = values().firstOrNull { it.key == key } ?: SIM_CALL
    }
}
