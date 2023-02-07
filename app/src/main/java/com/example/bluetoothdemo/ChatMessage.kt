package com.example.bluetoothdemo

import androidx.recyclerview.widget.DiffUtil

data class ChatMessage(
    val message: String,
    val isWrite: Boolean
) {
}

class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.message == newItem.message
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}