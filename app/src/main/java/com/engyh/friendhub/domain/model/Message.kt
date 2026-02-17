package com.engyh.friendhub.domain.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val imageName: String = "",
    val audioUrl: String = "",
    val duration: Long = 0L,
    val type: String = "",
    val time: Long = System.currentTimeMillis()
)