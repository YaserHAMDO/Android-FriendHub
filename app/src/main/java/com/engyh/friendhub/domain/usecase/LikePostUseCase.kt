package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LikePostUseCase @Inject constructor(
    private val repository: PostsRepository
) {
    suspend operator fun invoke(postId: String,
                                postOwnerId: String,
                                liked: Boolean): Flow<Response<Boolean>> {
        return repository.likePost(postId, postOwnerId, liked)
    }
}