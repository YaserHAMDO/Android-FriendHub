package com.engyh.friendhub.presentation.viewmodel

import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val friendsCount: Int = 0,
    val postsCount: Int = 0,
    val posts: List<Post> = emptyList()
)
