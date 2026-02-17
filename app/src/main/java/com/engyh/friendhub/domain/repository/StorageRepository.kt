package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun uploadImage(uri: String, path: String): Flow<Response<String>>
}
