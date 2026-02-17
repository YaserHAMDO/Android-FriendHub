package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Chat
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.usecase.GetChatListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val getChatListUseCase: GetChatListUseCase,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _chatsState = MutableStateFlow<Response<List<Chat>>>(Response.Loading)
    val chatsState: StateFlow<Response<List<Chat>>> = _chatsState.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true

        viewModelScope.launch {
            val userId = authRepository.userUid()
            if (userId.isBlank()) {
                _chatsState.value = Response.Success(emptyList())
                return@launch
            }

            _chatsState.value = Response.Loading

            getChatListUseCase(userId)
                .catch { e ->
                    _chatsState.value = Response.Error(e.message ?: "Failed to load chats")
                }
                .collect { list ->
                    _chatsState.value = Response.Success(list)
                }
        }
    }
}