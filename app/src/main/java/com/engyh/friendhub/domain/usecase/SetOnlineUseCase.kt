package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.PresenceRepository
import javax.inject.Inject

class SetOnlineUseCase @Inject constructor(
    private val repo: PresenceRepository
) {
    suspend operator fun invoke() = repo.setOnline()
}