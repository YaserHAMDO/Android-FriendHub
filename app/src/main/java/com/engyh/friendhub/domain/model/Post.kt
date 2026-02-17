package com.engyh.friendhub.domain.model

import com.google.firebase.Timestamp

data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentCount: Int = 0
)
