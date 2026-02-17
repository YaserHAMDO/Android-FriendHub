package com.engyh.friendhub.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Comment
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.domain.usecase.AddCommentUseCase
import com.engyh.friendhub.domain.usecase.CreatePostUseCase
import com.engyh.friendhub.domain.usecase.GetCommentsUseCase
import com.engyh.friendhub.domain.usecase.GetPostsUseCase
import com.engyh.friendhub.domain.usecase.LikePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val getPostsUseCase: GetPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase
) : ViewModel() {

    private val _postsState = MutableStateFlow<Response<List<Post>>>(Response.Loading)
    val postsState: StateFlow<Response<List<Post>>> = _postsState.asStateFlow()

    private val _createPostState = MutableStateFlow<Response<Boolean>?>(null)
    val createPostState: StateFlow<Response<Boolean>?> = _createPostState.asStateFlow()

    private val _commentsState = MutableStateFlow<Response<List<Comment>>?>(null)
    val commentsState: StateFlow<Response<List<Comment>>?> = _commentsState.asStateFlow()

    private var started = false
    private var postsJob: Job? = null

    fun start() {
        if (started) return
        started = true
        collectPosts()
    }

    fun refreshPosts() {
        collectPosts(restart = true)
    }

    private fun collectPosts(restart: Boolean = false) {
        if (restart) {
            postsJob?.cancel()
            postsJob = null
        }
        if (postsJob != null) return

        postsJob = viewModelScope.launch {
            _postsState.value = Response.Loading
            getPostsUseCase().collect { _postsState.value = it }
        }
    }

    fun createPost(text: String, imageUri: Uri?) {
        viewModelScope.launch {
            _createPostState.value = Response.Loading
            createPostUseCase(text, imageUri.toString()).collect { result ->
                _createPostState.value = result
            }
        }
    }

    fun likePost(postId: String, postOwnerId: String, liked: Boolean) {
        viewModelScope.launch {
            likePostUseCase(postId, postOwnerId, liked).collect {}
        }
    }

    fun getComments(postId: String, postOwnerId: String) {
        viewModelScope.launch {
            _commentsState.value = Response.Loading
            getCommentsUseCase(postId, postOwnerId).collect { _commentsState.value = it }
        }
    }

    fun addComment(postId: String, postOwnerId: String, commentText: String) {
        viewModelScope.launch {
            addCommentUseCase(postId, postOwnerId, commentText).collect { result ->
                if (result is Response.Success) {
                    getComments(postId, postOwnerId)
                }
            }
        }
    }

    fun resetCreatePostState() {
        _createPostState.value = null
    }

    fun clearCommentsState() {
        _commentsState.value = null
    }
}