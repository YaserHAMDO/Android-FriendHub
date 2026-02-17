package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.domain.model.Chat
import kotlinx.coroutines.flow.Flow

interface ChatListRepository {
    fun getChatList(userId: String): Flow<List<Chat>>
}