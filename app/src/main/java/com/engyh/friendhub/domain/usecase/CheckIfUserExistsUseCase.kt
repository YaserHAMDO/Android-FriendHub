package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import javax.inject.Inject

class CheckIfUserExistsUseCase @Inject constructor(
    private val repository: AuthenticationRepository
) {
    suspend operator fun invoke(uid: String): Boolean {
        return repository.checkIfUserExists(uid)
    }
}