package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.FriendsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFriendsUseCase @Inject constructor(
    private val repository: FriendsRepository
) {
    operator fun invoke(userId: String): Flow<List<User>> {
        return repository.getFriends(userId)
    }
}