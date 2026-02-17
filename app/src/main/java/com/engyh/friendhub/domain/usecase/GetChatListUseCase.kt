package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.model.Chat
import com.engyh.friendhub.domain.repository.ChatListRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChatListUseCase @Inject constructor(
    private val repository: ChatListRepository
) {
    operator fun invoke(userId: String): Flow<List<Chat>> {
        return repository.getChatList(userId)
    }
}