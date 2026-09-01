package com.fotocontact.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fotocontact.app.R
import com.fotocontact.app.data.Feature
import com.fotocontact.app.data.Rule
import com.fotocontact.app.util.Photos

class RuleAdapter(
    private var items: List<Rule>,
    private val onClick: (Rule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.avatar)
        val name: TextView = v.findViewById(R.id.name)
        val detail: TextView = v.findViewById(R.id.detail)
    }

    fun submit(list: List<Rule>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rule, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = items[position]
        holder.name.text = r.displayName().ifBlank { "(tanpa nama)" }

        val aktif = Feature.values().filter { r.cfgFor(it).enabled }
        val label = if (aktif.isEmpty()) "Tidak ada fitur aktif"
        else aktif.joinToString(" · ") {
            when (it) {
                Feature.SIM_CALL -> "SIM"
                Feature.WA_VOICE -> "WA suara"
                Feature.WA_VIDEO -> "WA video"
                Feature.WA_MESSAGE -> "WA pesan"
            }
        }
        holder.detail.text = label

        val bmp = Photos.load(r.callPhoto ?: r.msgPhoto)
        if (bmp != null) {
            holder.avatar.setImageBitmap(Photos.circle(bmp, 220))
        } else {
            holder.avatar.setImageResource(R.drawable.bg_placeholder)
        }

        holder.itemView.setOnClickListener { onClick(r) }
    }
}
