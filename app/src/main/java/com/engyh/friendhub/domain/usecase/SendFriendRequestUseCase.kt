package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.FriendsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendFriendRequestUseCase @Inject constructor(
    private val repository: FriendsRepository
) {
    operator fun invoke(currentUserId: String, targetUser: User): Flow<Response<Boolean>> {
        return repository.sendFriendRequest(currentUserId, targetUser)
    }
}