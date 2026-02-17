package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.usecase.AcceptFriendRequestUseCase
import com.engyh.friendhub.domain.usecase.DeleteFriendRequestUseCase
import com.engyh.friendhub.domain.usecase.GetFriendRequestsUseCase
import com.engyh.friendhub.domain.usecase.GetFriendsUseCase
import com.engyh.friendhub.domain.usecase.SearchUsersUseCase
import com.engyh.friendhub.domain.usecase.SendFriendRequestUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val getFriendRequestsUseCase: GetFriendRequestsUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val deleteFriendRequestUseCase: DeleteFriendRequestUseCase,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Response<FriendsUiState>>(Response.Loading)
    val uiState: StateFlow<Response<FriendsUiState>> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow<Response<List<User>>>(Response.Success(emptyList()))
    val searchState: StateFlow<Response<List<User>>> = _searchState.asStateFlow()

    private val _operationEvents = MutableSharedFlow<Response<Boolean>>(extraBufferCapacity = 1)
    val operationEvents = _operationEvents.asSharedFlow()

    private var started = false
    private var startJob: Job? = null
    private var currentUserId: String? = null

    fun start() {
        if (started) return
        started = true

        startJob?.cancel()
        startJob = viewModelScope.launch {
            val uid = authRepository.userUid()
            currentUserId = uid

            if (uid.isBlank()) {
                _uiState.value = Response.Success(FriendsUiState())
                return@launch
            }

            _uiState.value = Response.Loading

            combine(
                getFriendsUseCase(uid),
                getFriendRequestsUseCase(uid)
            ) { friends, requests ->
                FriendsUiState(friends = friends, requests = requests)
            }
                .catch { e ->
                    _uiState.value = Response.Error(e.message ?: "Failed to load friends")
                }
                .collect { state ->
                    _uiState.value = Response.Success(state)
                }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _searchState.value = Response.Loading
            runCatching {
                val result = searchUsersUseCase(query).first()
                _searchState.value = Response.Success(result)
            }.onFailure { e ->
                _searchState.value = Response.Error(e.message ?: "Search failed")
            }
        }
    }

    fun sendRequest(user: User) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            sendFriendRequestUseCase(uid, user).collect { _operationEvents.tryEmit(it) }
        }
    }

    fun acceptRequest(user: User) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            acceptFriendRequestUseCase(uid, user).collect { _operationEvents.tryEmit(it) }
        }
    }

    fun rejectRequest(user: User) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            deleteFriendRequestUseCase(uid, user).collect { _operationEvents.tryEmit(it) }
        }
    }
}