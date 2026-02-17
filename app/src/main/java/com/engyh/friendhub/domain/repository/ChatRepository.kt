package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(senderId: String, receiverId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Response<Boolean>
    suspend fun uploadVoice(uriString: String): Response<String>

    suspend fun setTyping(senderId: String, receiverId: String, isTyping: Boolean)
    fun observeTyping(senderId: String, receiverId: String): Flow<Boolean>

    suspend fun clearChatForUser(currentUserId: String, otherUserId: String): Response<Boolean>
    suspend fun resetUnread(currentUserId: String, otherUserId: String)

    suspend fun updateChatSummary(senderId: String, receiverId: String, lastMessage: String)
}