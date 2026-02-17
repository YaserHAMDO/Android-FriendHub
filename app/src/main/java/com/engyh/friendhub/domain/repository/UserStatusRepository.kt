package com.engyh.friendhub.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserStatusRepository {
    fun observeOnline(userId: String): Flow<Boolean>
}
