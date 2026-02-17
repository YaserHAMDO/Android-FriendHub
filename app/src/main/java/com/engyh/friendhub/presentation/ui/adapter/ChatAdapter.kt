package com.engyh.friendhub.presentation.ui.adapter

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.engyh.friendhub.R
import com.engyh.friendhub.databinding.ItemMessageBinding
import com.engyh.friendhub.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val currentUserId: String,
    private val listener: Listener
) : ListAdapter<Message, ChatAdapter.ViewHolder>(Diff) {

    interface Listener {
        fun onAudioToggle(message: Message)
        fun onImageClick(message: Message)
    }

    private var playingMessageId: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    class ViewHolder(
        val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        var counterRunnable: Runnable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMessageBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.counterRunnable?.let { mainHandler.removeCallbacks(it) }
        holder.counterRunnable = null
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        holder.counterRunnable?.let { mainHandler.removeCallbacks(it) }
        holder.counterRunnable = null

        binding.messageTimeTextView.text =
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(item.time))

        binding.messageTextView.isVisible(false)
        binding.voiceLinearLayout.isVisible(false)
        binding.imageLinearLayout.isVisible(false)
        binding.durationTextView.isVisible(false)

        when (item.type) {
            "text" -> {
                binding.messageTextView.isVisible(true)
                binding.messageTextView.text = item.text
            }

            "image" -> {
                binding.imageLinearLayout.isVisible(true)

                if (item.text.isNotBlank()) {
                    binding.messageTextView.isVisible(true)
                    binding.messageTextView.text = item.text
                }

                Glide.with(holder.itemView).load(item.imageUrl).into(binding.previewImageView)
                binding.previewImageView.setOnClickListener { listener.onImageClick(item) }
            }

            "audio" -> {
                binding.voiceLinearLayout.isVisible(true)
                binding.durationTextView.isVisible(true)

                val total = toTime(item.duration)
                val isPlaying = (item.messageId == playingMessageId)

                if (isPlaying) {
                    Glide.with(holder.itemView).asGif().load(R.drawable.sound).into(binding.soundGifImageView)
                    binding.playSoundImageView.setBackgroundResource(R.drawable.baseline_pause_24)
                    binding.mainLinearLayout.setBackgroundResource(R.drawable.playing_message_background)

                    startCounter(holder, durationMs = item.duration, binding.durationTextView)
                } else {
                    Glide.with(holder.itemView).load(R.drawable.dotted_line).into(binding.soundGifImageView)
                    binding.playSoundImageView.setBackgroundResource(R.drawable.baseline_play_arrow_24)
                    binding.durationTextView.text = "00:00/$total"
                }

                binding.playSoundImageView.setOnClickListener { listener.onAudioToggle(item) }
            }
        }

        val params = binding.mainLinearLayout.layoutParams as LinearLayout.LayoutParams
        val context = holder.itemView.context

        if (item.senderId == currentUserId) {
            binding.messageTextView.setTextColor(context.getColor(R.color.black))
            binding.messageTimeTextView.setTextColor(context.getColor(R.color.color4))
            params.gravity = Gravity.END

            if (!(item.type == "audio" && item.messageId == playingMessageId)) {
                binding.mainLinearLayout.setBackgroundResource(R.drawable.sent_message_background)
            }
        } else {
            binding.messageTextView.setTextColor(context.getColor(R.color.color7))
            binding.messageTimeTextView.setTextColor(context.getColor(R.color.color9))
            params.gravity = Gravity.START

            if (!(item.type == "audio" && item.messageId == playingMessageId)) {
                binding.mainLinearLayout.setBackgroundResource(R.drawable.received_message_background)
            }
        }

        binding.mainLinearLayout.layoutParams = params
    }

    fun setPlayingMessage(messageId: String?) {
        val old = playingMessageId
        playingMessageId = messageId

        if (old != null) {
            val i = currentList.indexOfFirst { it.messageId == old }
            if (i != -1) notifyItemChanged(i)
        }
        if (messageId != null) {
            val i = currentList.indexOfFirst { it.messageId == messageId }
            if (i != -1) notifyItemChanged(i)
        }
    }

    private fun startCounter(holder: ViewHolder, durationMs: Long, durationTextView: TextView) {
        var counter = 0
        val max = (durationMs / 1000).toInt()
        val total = toTime(durationMs)

        val runnable = object : Runnable {
            override fun run() {
                if (counter <= max) {
                    durationTextView.text =
                        String.format("%02d:%02d/%s", counter / 60, counter % 60, total)
                    counter++
                    mainHandler.postDelayed(this, 1000)
                }
            }
        }

        holder.counterRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun toTime(ms: Long): String {
        val s = ms / 1000
        val m = s / 60
        val rem = s % 60
        return String.format("%02d:%02d", m, rem)
    }

    private fun View.isVisible(v: Boolean) {
        visibility = if (v) View.VISIBLE else View.GONE
    }

    private object Diff : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) =
            oldItem.messageId == newItem.messageId

        override fun areContentsTheSame(oldItem: Message, newItem: Message) =
            oldItem == newItem
    }
}