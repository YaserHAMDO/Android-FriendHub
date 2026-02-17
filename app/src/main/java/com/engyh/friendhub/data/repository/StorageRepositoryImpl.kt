package com.engyh.friendhub.data.repository

import androidx.core.net.toUri
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.StorageRepository
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {

    override fun uploadImage(uri: String, path: String): Flow<Response<String>> = flow {
        emit(Response.Loading)
        try {
            val ref = storage.reference.child(path)
            ref.putFile(uri.toUri()).await()
            val url = ref.downloadUrl.await().toString()
            emit(Response.Success(url))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Upload failed"))
        }
    }
}