package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Response<Boolean> {
        return userRepository.updateUser(user)
    }
}
