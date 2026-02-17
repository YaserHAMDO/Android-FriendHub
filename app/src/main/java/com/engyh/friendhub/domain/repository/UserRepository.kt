package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User

import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(uid: String): Flow<User?>
    suspend fun updateUser(user: User): Response<Boolean>

}
