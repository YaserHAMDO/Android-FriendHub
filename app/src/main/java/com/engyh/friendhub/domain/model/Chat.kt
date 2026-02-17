package com.engyh.friendhub.domain.model

import com.google.firebase.Timestamp

data class Chat(
    var userId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    var lastMessage: String = "",
    var unreadMessages: Long = 0,
    var lastMessageDate: Timestamp = Timestamp.now()
)
