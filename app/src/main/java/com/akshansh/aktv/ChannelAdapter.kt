package com.akshansh.aktv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ChannelAdapter(private val onClick: (Channel) -> Unit) :
    RecyclerView.Adapter<ChannelAdapter.Holder>() {

    private var items = emptyList<Channel>()

    fun submit(newItems: List<Channel>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val logo: ImageView = view.findViewById(R.id.channelLogo)
        private val title: TextView = view.findViewById(R.id.channelTitle)

        init {
            view.setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.06f else 1f)
                    .scaleY(if (focused) 1.06f else 1f).setDuration(120).start()
            }
            view.setOnClickListener {
                val p = bindingAdapterPosition
                if (p != RecyclerView.NO_POSITION) onClick(items[p])
            }
        }

        fun bind(channel: Channel) {
            title.text = channel.name
            logo.setImageResource(android.R.drawable.ic_menu_tv)
            channel.logo?.let {
                if (it.startsWith("http")) Picasso.get().load(it).fit().centerInside()
                    .placeholder(android.R.drawable.ic_menu_tv).into(logo)
            }
        }
    }
}
