package com.engyh.friendhub.domain.repository

interface ConnectionsRepository {
    suspend fun getFriendIds(userId: String): List<String>
    suspend fun getRequestIds(userId: String): List<String>
}
