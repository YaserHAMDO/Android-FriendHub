package com.engyh.friendhub.core

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)