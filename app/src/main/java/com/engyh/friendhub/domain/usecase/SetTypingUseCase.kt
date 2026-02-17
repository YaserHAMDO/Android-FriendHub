package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.ChatRepository
import javax.inject.Inject

class SetTypingUseCase @Inject constructor(
    private val repo: ChatRepository
) {
    suspend operator fun invoke(senderId: String, receiverId: String, isTyping: Boolean) {
        repo.setTyping(senderId, receiverId, isTyping)
    }
}