package com.engyh.friendhub.presentation.viewmodel

import android.location.Location
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
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class UserInfoViewModel @Inject constructor(
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val userRepository: UserRepository,
    private val friendsRepository: FriendsRepository,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _postsState = MutableStateFlow<Response<List<Post>>>(Response.Loading)
    val postsState: StateFlow<Response<List<Post>>> = _postsState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)

    private val _friendsCount = MutableStateFlow(0)
    val friendsCount: StateFlow<Int> = _friendsCount.asStateFlow()

    private val _postsCount = MutableStateFlow(0)
    val postsCount: StateFlow<Int> = _postsCount.asStateFlow()

    private val _distance = MutableStateFlow<Float?>(null)
    val distance: StateFlow<Float?> = _distance.asStateFlow()


    var currentUserId: String? = null

    init {
        viewModelScope.launch {
            combine(_user, _currentUser) { user, currentUser ->
                if (user != null && currentUser != null) {
                    val userLocation = Location("").apply {
                        latitude = user.lat
                        longitude = user.lon
                    }
                    val currentUserLocation = Location("").apply {
                        latitude = currentUser.lat
                        longitude = currentUser.lon
                    }
                    val distanceInMeters = currentUserLocation.distanceTo(userLocation)
                    _distance.value = distanceInMeters
                }
            }.collect{}
        }
    }



    fun loadCurrentAndTargetUserProfiles(userId: String) {
        viewModelScope.launch {
            userRepository.getUser(userId).collect {
                _user.value = it
            }
        }
        viewModelScope.launch {
            val currentUserUid = authRepository.userUid()
            currentUserId = currentUserUid

            userRepository.getUser(currentUserUid).collect {
                _currentUser.value = it


            }
        }

        viewModelScope.launch {
            friendsRepository.getFriends(userId).collect {
                _friendsCount.value = it.size
            }
        }

        viewModelScope.launch {
                    try {
                        val snapshot = firestore.collection("users").document(userId).collection("posts")
                            .get()
                            .await()
                        _postsCount.value = snapshot.size()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


    }


    fun fetchUserPosts(userId: String) {
        viewModelScope.launch {
            _postsState.value = Response.Loading
            getUserPostsUseCase(userId).collect {
                _postsState.value = it
            }
        }
    }

    fun likePost(postId: String, postOwnerId: String, liked: Boolean) {
        viewModelScope.launch {
            likePostUseCase(postId, postOwnerId, liked).collect {
            }
        }
    }
}
