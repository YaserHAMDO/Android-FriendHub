package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.repository.FriendsRepository
import com.engyh.friendhub.domain.repository.UserRepository
import com.engyh.friendhub.domain.usecase.GetUserPostsUseCase
import com.engyh.friendhub.domain.usecase.LikePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val userRepository: UserRepository,
    private val authRepository: AuthenticationRepository,
    private val friendsRepository: FriendsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<Response<ProfileUiState>>(Response.Loading)
    val uiState: StateFlow<Response<ProfileUiState>> = _uiState.asStateFlow()

    private var started = false
    private var job: Job? = null
    private var currentUserId: String? = null

    fun start() {
        if (started) return
        started = true

        job?.cancel()
        job = viewModelScope.launch {
            val uid = authRepository.userUid()
            currentUserId = uid

            if (uid.isBlank()) {
                _uiState.value = Response.Error("User not logged in")
                return@launch
            }

            _uiState.value = Response.Loading

            val userFlow = userRepository.getUser(uid)
            val friendsFlow = friendsRepository.getFriends(uid)
            val postsFlow = getUserPostsUseCase(uid)

            combine(userFlow, friendsFlow, postsFlow) { user: User?, friends: List<User>, postsResp: Response<List<Post>> ->
                when (postsResp) {
                    is Response.Loading -> Response.Loading
                    is Response.Error -> Response.Error(postsResp.message)
                    is Response.Success -> {
                        val posts = postsResp.data
                        Response.Success(
                            ProfileUiState(
                                user = user,
                                friendsCount = friends.size,
                                postsCount = posts.size,
                                posts = posts
                            )
                        )
                    }
                }
            }
                .catch { e ->
                    _uiState.value = Response.Error(e.message ?: "Failed to load profile")
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun likePost(postId: String, postOwnerId: String, liked: Boolean) {
        viewModelScope.launch {
            likePostUseCase(postId, postOwnerId, liked).collect { }
        }
    }
}