package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTypingUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    operator fun invoke(senderId: String, receiverId: String): Flow<Boolean> {
        return repo.observeTyping(senderId, receiverId)
    }
}