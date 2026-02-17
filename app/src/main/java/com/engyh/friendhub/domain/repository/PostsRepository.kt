package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Comment
import com.engyh.friendhub.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostsRepository {

    fun getPosts(): Flow<Response<List<Post>>>

    fun getUserPosts(userId: String): Flow<Response<List<Post>>>

    suspend fun createPost(text: String, imageUri: String?): Flow<Response<Boolean>>

    suspend fun likePost(postId: String,
                         postOwnerId: String,
                         liked: Boolean): Flow<Response<Boolean>>

    fun getComments(postId: String, postOwnerId: String): Flow<Response<List<Comment>>>

    suspend fun addComment(
        postId: String,
        postOwnerId: String,
        commentText: String
    ): Flow<Response<Boolean>>

}
