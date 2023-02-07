package com.example.bluetoothdemo.chat

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.ChatMessage
import com.example.bluetoothdemo.ChatMessageDiffCallback
import com.example.bluetoothdemo.R

class ChatMsgAdapter: ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {
    class ChatDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val msg: TextView = view.findViewById(R.id.msg)

        fun bind(message: ChatMessage) {
            msg.text = message.message
            if (message.isWrite) msg.setTextColor(itemView.resources.getColor(R.color.black, null))
            else msg.setTextColor(itemView.resources.getColor(R.color.purple_700, null))
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