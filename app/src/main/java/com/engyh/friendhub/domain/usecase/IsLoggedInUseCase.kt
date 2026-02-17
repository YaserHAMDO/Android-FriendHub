package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import javax.inject.Inject

class IsLoggedInUseCase @Inject constructor(
    private val repo: AuthenticationRepository
) {
    suspend operator fun invoke(): Boolean = repo.isLoggedIn()
}
