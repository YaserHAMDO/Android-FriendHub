package com.engyh.friendhub.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.engyh.friendhub.databinding.ItemCommentBinding
import com.engyh.friendhub.domain.model.Comment
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.ImageLoader

class CommentAdapter() : ListAdapter<Comment, CommentAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Comment) {
            binding.commentTextView.text = item.commentText
            binding.timeTextView.text = formatTimestamp(item.timestamp)
            binding.userNameTextView.text = item.userName

            ImageLoader.loadUserImage(
                imageView = binding.userImage,
                url = item.userImage,
                progress = binding.progressBar,
            )
        }
    }

    private object Diff : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment) = oldItem.commentId == newItem.commentId
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment) = oldItem == newItem
    }
}
