package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.model.Message
import com.engyh.friendhub.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: Message, senderId: String, receiverId: String) {
        repository.sendMessage(message)

        val last = when (message.type) {
            "text" -> message.text
            "image" -> "Sent an Image"
            "audio" -> "Voice message"
            else -> message.text
        }

        repository.updateChatSummary(
            senderId = senderId,
            receiverId = receiverId,
            lastMessage = last
        )
    }
}
