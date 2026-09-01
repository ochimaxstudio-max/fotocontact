package com.fotocontact.app.overlay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fotocontact.app.R
import com.fotocontact.app.util.Photos

class PeekAdapter(private val items: List<MessageBuffer.Item>) :
    RecyclerView.Adapter<PeekAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.avatar)
        val name: TextView = v.findViewById(R.id.name)
        val line: TextView = v.findViewById(R.id.line)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_peek, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.line.text = item.line
        val bmp = Photos.load(item.photoPath)
        if (bmp != null) {
            holder.avatar.setImageBitmap(Photos.circle(bmp, 220))
        } else {
            holder.avatar.setImageResource(R.drawable.bg_placeholder)
        }
    }
}
