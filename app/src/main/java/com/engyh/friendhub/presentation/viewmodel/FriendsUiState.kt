package com.engyh.friendhub.presentation.viewmodel

import com.engyh.friendhub.domain.model.User

data class FriendsUiState(
    val friends: List<User> = emptyList(),
    val requests: List<User> = emptyList()
)
