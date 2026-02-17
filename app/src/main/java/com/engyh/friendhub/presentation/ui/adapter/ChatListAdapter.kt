package com.engyh.friendhub.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.engyh.friendhub.databinding.ItemChatBinding
import com.engyh.friendhub.domain.model.Chat
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.ImageLoader

class ChatListAdapter(
    private val onItemClick: (Chat) -> Unit
) : ListAdapter<Chat, ChatListAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: Chat) {
            binding.userName.text = user.name
            binding.lastMessageTextView.text = user.lastMessage
            binding.messageTimeTextView.text = formatTimestamp(user.lastMessageDate)
            binding.unreadMessageTextView.isVisible = user.unreadMessages > 0
            binding.unreadMessageTextView.text = user.unreadMessages.toString()

            ImageLoader.loadUserImage(
                imageView = binding.userImage,
                url = user.imageUrl,
                progress = binding.imageProgressBar,
            )

            binding.root.setOnClickListener { onItemClick(user) }
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<Chat>() {
        override fun areItemsTheSame(oldItem: Chat, newItem: Chat) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Chat, newItem: Chat) = oldItem == newItem
    }
}