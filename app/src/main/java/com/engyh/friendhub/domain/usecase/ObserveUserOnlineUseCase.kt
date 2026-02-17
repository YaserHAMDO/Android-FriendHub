package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.PresenceRepository
import javax.inject.Inject

class ObserveUserOnlineUseCase @Inject constructor(
    private val repo: PresenceRepository
) {
    operator fun invoke(userId: String) = repo.observeIsOnline(userId)
}
