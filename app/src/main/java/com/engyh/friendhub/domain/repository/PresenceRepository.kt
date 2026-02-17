package com.engyh.friendhub.domain.repository

interface PresenceRepository {
    suspend fun startPresence()
    suspend fun setOnline()
    suspend fun setOffline()
    fun observeIsOnline(userId: String): kotlinx.coroutines.flow.Flow<Boolean>
}
