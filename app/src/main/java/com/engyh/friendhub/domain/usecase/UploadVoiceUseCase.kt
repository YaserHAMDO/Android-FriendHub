package com.engyh.friendhub.domain.usecase


import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.ChatRepository
import javax.inject.Inject

class UploadVoiceUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(uriString: String): Response<String> {
        return repository.uploadVoice(uriString)
    }
}