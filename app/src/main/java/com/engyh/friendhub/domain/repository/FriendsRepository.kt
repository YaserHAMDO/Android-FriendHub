package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    fun getFriendRequests(userId: String): Flow<List<User>>
    fun getFriends(userId: String): Flow<List<User>>
    fun searchUsers(query: String): Flow<List<User>>
    fun sendFriendRequest(currentUserId: String, targetUser: User): Flow<Response<Boolean>>
    fun acceptFriendRequest(currentUserId: String, targetUser: User): Flow<Response<Boolean>>
    fun deleteFriendRequest(currentUserId: String, targetUser: User): Flow<Response<Boolean>>
}