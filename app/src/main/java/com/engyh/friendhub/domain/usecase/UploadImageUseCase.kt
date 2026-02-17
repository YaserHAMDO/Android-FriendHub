package com.engyh.friendhub.domain.usecase


import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(uri: String, path: String): Flow<Response<String>> {
        return storageRepository.uploadImage(uri, path)
    }
}
