package com.engyh.friendhub.data.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateUser(user: User): Response<Boolean> {
        return try {
            firestore.collection("users").document(user.userId)
                .set(user)
                .await()
            Response.Success(true)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Oops, something went wrong.")
        }
    }
}