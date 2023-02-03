package com.example.bluetoothdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ChatMsgAdapter: ListAdapter<String, RecyclerView.ViewHolder>(ChatMsgDiffCallback()) {
    class ChatDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val msg: TextView = view.findViewById(R.id.msg)

        fun bind(message: String) {
            msg.text = message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_msg, parent, false)
        return ChatDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChatDetailViewHolder -> {
                holder.bind(getItem(position))
            }
        }
    }
}

class ChatMsgDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}