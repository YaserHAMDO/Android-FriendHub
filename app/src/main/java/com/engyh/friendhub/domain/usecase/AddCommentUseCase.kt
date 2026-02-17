package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.PostsRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {
    suspend operator fun invoke(postId: String, postOwnerId: String, commentText: String) = postsRepository.addComment(postId = postId, postOwnerId = postOwnerId, commentText = commentText)
}
