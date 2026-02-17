package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.PostsRepository
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    operator fun invoke(postId: String, postOwnerId: String) = postsRepository.getComments(postId, postOwnerId)
}
