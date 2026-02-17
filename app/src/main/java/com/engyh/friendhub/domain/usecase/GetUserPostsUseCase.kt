package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserPostsUseCase @Inject constructor(
    private val repository: PostsRepository
) {
    operator fun invoke(userId: String): Flow<Response<List<Post>>> {
        return repository.getUserPosts(userId)
    }
}
