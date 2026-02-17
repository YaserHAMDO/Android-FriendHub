package com.engyh.friendhub.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.ItemPostBinding
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.presentation.util.DateTimeUtils.formatTimestamp
import com.engyh.friendhub.presentation.util.ImageLoader

class PostAdapter(
    private val onUserImageClick: (String) -> Unit,
    private val onLikeClick: (String, String, Boolean) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onImageClick: (Post, ImageView) -> Unit,
    private val onShareClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.postTextView.text = post.text
            binding.userNameTextView.text = post.userName
            binding.timeTextView.text = formatTimestamp(post.timestamp)
            binding.likesCountTextView.text = "${post.likesCount} Likes, ${post.commentCount} Comments"

            ImageLoader.loadUserImage(
                imageView = binding.userImage,
                url = post.userImageUrl,
                progress = binding.imageProgressBar,
            )

            if (post.imageUrl.isNotBlank()) {
                binding.postBackgroundConstraintLayout.isVisible = true

                ImageLoader.loadPostImage(
                    imageView = binding.postImageView,
                    url = post.imageUrl,
                    progressBar = binding.progressBar,
                    binding.postBackgroundConstraintLayout,
                    itemView.context,
                    action = { onImageClick(post, binding.postImageView) }
                )
            } else {
                binding.postBackgroundConstraintLayout.isVisible = false
            }

            binding.likeImageView.setImageResource(
                if (post.isLikedByMe) R.drawable.liked_icon else R.drawable.not_liked_icon
            )
            binding.likeLinearLayout.setOnClickListener {
                onLikeClick(post.postId, post.userId, !post.isLikedByMe)
            }

            binding.userImage.setOnClickListener {
                onUserImageClick(post.userId)
            }

            binding.commentLinearLayout.setOnClickListener { onCommentClick(post) }
            binding.shareLinearLayout.setOnClickListener { onShareClick(post) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) =
            oldItem.postId == newItem.postId && oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}