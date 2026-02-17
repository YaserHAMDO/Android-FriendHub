package com.engyh.friendhub.data.repository

import com.engyh.friendhub.domain.repository.ConnectionsRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ConnectionsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ConnectionsRepository {

    override suspend fun getFriendIds(userId: String): List<String> {
        return firestore.collection("users")
            .document(userId)
            .collection("friends")
            .get()
            .await()
            .documents
            .map { it.id }
    }

    override suspend fun getRequestIds(userId: String): List<String> {
        return firestore.collection("users")
            .document(userId)
            .collection("requests")
            .get()
            .await()
            .documents
            .map { it.id }
    }
}
