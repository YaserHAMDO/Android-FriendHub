package com.engyh.friendhub.domain.model

import com.google.firebase.Timestamp

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val userImage: String = "",
    val userName: String = "",
    val commentText: String = "",
    val timestamp: Timestamp = Timestamp.now()

)
