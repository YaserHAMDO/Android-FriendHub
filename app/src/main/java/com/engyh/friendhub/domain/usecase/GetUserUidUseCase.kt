package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserUidUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {
    suspend operator fun invoke() = flow { emit(authenticationRepository.userUid()) }
}