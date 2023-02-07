package com.example.bluetoothdemo.chat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdemo.ChatMessage
import com.example.bluetoothdemo.ChatMessageDiffCallback
import com.example.bluetoothdemo.R

class ChatMsgAdapter: ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {
    class ChatDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val msg: TextView = view.findViewById(R.id.msg)
        private val chatLayout: LinearLayout = view.findViewById(R.id.chatLayout)

        fun bind(message: ChatMessage) {
            msg.text = message.message
            if (message.isWrite) {
                chatLayout.gravity = Gravity.END
                msg.background = ResourcesCompat.getDrawable(itemView.resources, R.drawable.write_msg_bg, null)
            } else {
                chatLayout.gravity = Gravity.START
                msg.background = ResourcesCompat.getDrawable(itemView.resources, R.drawable.read_msg_bg, null)
            }
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