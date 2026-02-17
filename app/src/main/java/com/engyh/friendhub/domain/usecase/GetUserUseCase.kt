package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.UserRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(uid: String) = userRepository.getUser(uid)
}
