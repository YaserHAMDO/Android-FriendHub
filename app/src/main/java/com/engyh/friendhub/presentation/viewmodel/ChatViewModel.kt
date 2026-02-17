package com.engyh.friendhub.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Message
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.repository.ChatRepository
import com.engyh.friendhub.domain.usecase.GetMessagesUseCase
import com.engyh.friendhub.domain.usecase.ObserveTypingUseCase
import com.engyh.friendhub.domain.usecase.ObserveUserOnlineUseCase
import com.engyh.friendhub.domain.usecase.SendMessageUseCase
import com.engyh.friendhub.domain.usecase.SetTypingUseCase
import com.engyh.friendhub.domain.usecase.UploadImageUseCase
import com.engyh.friendhub.domain.usecase.UploadVoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val uploadVoiceUseCase: UploadVoiceUseCase,
    private val setTypingUseCase: SetTypingUseCase,
    private val observeTypingUseCase: ObserveTypingUseCase,
    private val observeUserOnline: ObserveUserOnlineUseCase,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _receiverId = MutableStateFlow<String?>(null)

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _typingStatus = MutableStateFlow(false)
    val typingStatus: StateFlow<Boolean> = _typingStatus.asStateFlow()

    private var messagesJob: Job? = null
    private var typingJob: Job? = null
    private var didResetUnread = false

    fun initChat(receiverId: String) {
        if (receiverId.isBlank()) return
        if (_receiverId.value == receiverId && _currentUserId.value != null) return

        _receiverId.value = receiverId

        viewModelScope.launch {
            val senderId = authRepository.userUid().takeIf { it.isNotBlank() }
            _currentUserId.value = senderId

            if (senderId == null) {
                _messages.value = emptyList()
                _typingStatus.value = false
                return@launch
            }

            startMessages(senderId, receiverId)
            startTypingObserver(senderId, receiverId)
        }
    }

    fun sendMessage(receiverId: String, text: String) {
        val senderId = _currentUserId.value ?: return
        val clean = text.trim()
        if (clean.isBlank()) return

        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = receiverId,
            text = clean,
            type = "text",
            time = System.currentTimeMillis()
        )

        viewModelScope.launch {
            sendMessageUseCase(message, senderId, receiverId)
            setTyping(false)
        }
    }

    fun sendImageMessage(receiverId: String, bitmap: Uri, caption: String) {
        val senderId = _currentUserId.value ?: return

        viewModelScope.launch {
            _uploading.value = true
            val uuid = UUID.randomUUID().toString()
            val path = "chat_images/$uuid"
            uploadImageUseCase(bitmap.toString(),  path).collect { response ->
                when (response) {
                    is Response.Success -> {
                        val imageUrl = response.data
                        val message = Message(
                            messageId = UUID.randomUUID().toString(),
                            senderId = senderId,
                            receiverId = receiverId,
                            text = caption.trim(),
                            imageUrl = imageUrl,
                            type = "image",
                            time = System.currentTimeMillis()
                        )
                        _uploading.value = false
                        sendMessageUseCase(message, senderId, receiverId)
                    }
                    is Response.Error -> {
                        _uploading.value = false
                    }
                    is Response.Loading -> {}
                }
            }

        }
    }

    fun sendVoiceMessage(receiverId: String, uri: Uri, duration: Long) {
        val senderId = _currentUserId.value ?: return

        viewModelScope.launch {
            _uploading.value = true
            val upload = uploadVoiceUseCase(uri.toString())
            if (upload is Response.Success) {
                val audioUrl = upload.data.orEmpty()
                val message = Message(
                    messageId = UUID.randomUUID().toString(),
                    senderId = senderId,
                    receiverId = receiverId,
                    audioUrl = audioUrl,
                    duration = duration,
                    type = "audio",
                    time = System.currentTimeMillis()
                )
                sendMessageUseCase(message, senderId, receiverId)
            }
            _uploading.value = false
        }
    }

    fun setTyping(isTyping: Boolean) {
        val senderId = _currentUserId.value ?: return
        val receiverId = _receiverId.value ?: return

        viewModelScope.launch {
            setTypingUseCase(senderId = senderId, receiverId = receiverId, isTyping = isTyping)
        }
    }

    private fun startMessages(senderId: String, receiverId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesUseCase(senderId, receiverId).collect { list ->
                _messages.value = list
                if (!didResetUnread) {
                    didResetUnread = true
                    runCatching { chatRepository.resetUnread(senderId, receiverId) }
                }

            }
        }
    }

    private fun startTypingObserver(senderId: String, receiverId: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            observeTypingUseCase(senderId = receiverId, receiverId = senderId).collect { isTyping ->
                _typingStatus.value = isTyping
            }
        }
    }

    fun friendOnlineState(userId: String) =
        observeUserOnline(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    override fun onCleared() {
        try { setTyping(false) } catch (_: Exception) {}
        super.onCleared()
    }

    fun clearChatForMe() {
        val senderId = _currentUserId.value ?: return
        val receiverId = _receiverId.value ?: return
        viewModelScope.launch {
            chatRepository.clearChatForUser(senderId, receiverId)
        }
    }
}