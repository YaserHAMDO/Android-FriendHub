package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.model.Message
import com.engyh.friendhub.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(senderId: String, receiverId: String): Flow<List<Message>> {
        return repository.getMessages(senderId, receiverId)
    }
}