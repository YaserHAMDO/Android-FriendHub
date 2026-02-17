package com.engyh.friendhub.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.engyh.friendhub.databinding.ItemUserBinding
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.presentation.util.ImageLoader

class UserAdapter(
    private val onItemClick: (User) -> Unit,
    private val onUserClick: (User) -> Unit,
    private val onUserRejectClick: (User) -> Unit,
    private val onSendRequestClick: (User) -> Unit,
    private val type: Int
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userNameTextView.text = user.name
            binding.userAboutTextView.text = user.about


            ImageLoader.loadUserImage(
                imageView = binding.userImage,
                url = user.imageUrl,
                progress = binding.imageProgressBar,
            )

            when (type) {
                0 -> {
                    binding.acceptImageView.isVisible = false
                    binding.rejectImageView.isVisible = false
                    binding.sendRequestImageView.isVisible = true
                }
                1 -> {
                    binding.acceptImageView.isVisible = true
                    binding.rejectImageView.isVisible = true
                    binding.sendRequestImageView.isVisible = false
                }
                else -> {
                    binding.acceptImageView.isVisible = false
                    binding.rejectImageView.isVisible = false
                    binding.sendRequestImageView.isVisible = false
                }
            }

            binding.acceptImageView.setOnClickListener {
                onUserClick(user)
            }

            binding.root.setOnClickListener {
                onItemClick(user)
            }

            binding.rejectImageView.setOnClickListener {
                onUserRejectClick(user)
            }

            binding.sendRequestImageView.setOnClickListener {
                onSendRequestClick(user)
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}