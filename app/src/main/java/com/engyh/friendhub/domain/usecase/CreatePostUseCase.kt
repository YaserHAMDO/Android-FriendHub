package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val repository: PostsRepository
) {
    suspend operator fun invoke(text: String, imageUri: String?): Flow<Response<Boolean>> {
        return repository.createPost(text, imageUri)
    }
}