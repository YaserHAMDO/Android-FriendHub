package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: AuthenticationRepository
) {
    suspend operator fun invoke(uid: String) = repository.getUserProfile(uid)
}