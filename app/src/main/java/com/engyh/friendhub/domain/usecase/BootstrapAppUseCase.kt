package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.repository.ConnectionsRepository
import javax.inject.Inject

class BootstrapAppUseCase @Inject constructor(
    private val authRepository: AuthenticationRepository,
    private val connectionsRepository: ConnectionsRepository
) {
    sealed class Result {
        data object LoggedOut : Result()
        data class Ready(val idsToPrefetch: List<String>) : Result()
    }

    suspend operator fun invoke(): Result {
        val myId = authRepository.userUid()
        if (myId.isBlank()) return Result.LoggedOut

        val exists = authRepository.checkIfUserExists(myId)
        if (!exists) {
            authRepository.logout()
            return Result.LoggedOut
        }

        val friendsIds = connectionsRepository.getFriendIds(myId)
        val requestIds = connectionsRepository.getRequestIds(myId)

        val all = (listOf(myId) + friendsIds + requestIds).distinct()
        return Result.Ready(all)
    }
}
