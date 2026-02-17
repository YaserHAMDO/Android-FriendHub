package com.engyh.friendhub.data.cache

import android.util.Log
import com.engyh.friendhub.domain.model.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileStore @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val cache = ConcurrentHashMap<String, User>()

    fun getCached(userId: String): User? = cache[userId]

    fun putAll(users: List<User>) {
        users.forEach { u ->
            val id = u.userId
            if (id.isNotBlank()) cache[id] = u
        }
    }

    fun clear() = cache.clear()

    suspend fun refresh(ids: List<String>) {
        val distinct = ids.distinct().filter { it.isNotBlank() }
        if (distinct.isEmpty()) return
        putAll(fetchUsersByIds(distinct))
    }

    suspend fun refreshMissing(ids: List<String>) {
        val missing = ids.distinct().filter { it.isNotBlank() && !cache.containsKey(it) }
        if (missing.isEmpty()) return
        putAll(fetchUsersByIds(missing))
    }

    private suspend fun fetchUsersByIds(ids: List<String>): List<User> =
        withContext(Dispatchers.IO) {
            val chunks = ids.chunked(10)

            coroutineScope {
                chunks.map { chunk ->
                    async {
                        val snap = firestore.collection("users")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()

                        snap.documents.mapNotNull { doc ->
                            val user = doc.toObject(User::class.java) ?: return@mapNotNull null
                            Log.d("UserProfileStore", "Fetched user: ${user.name} ${user.userId}")
                            user.copy(userId = doc.id)
                        }
                    }
                }.awaitAll().flatten()
            }
        }
}