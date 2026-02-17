package com.engyh.friendhub.data.repository

import android.util.Log
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.data.cache.UserProfileStore
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.FriendsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FriendsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileStore: UserProfileStore
) : FriendsRepository {

    override fun getFriendRequests(userId: String): Flow<List<User>> =
        observeIdsThenMapToProfiles(userId = userId, subCollection = "requests")

    override fun getFriends(userId: String): Flow<List<User>> =
        observeIdsThenMapToProfiles(userId = userId, subCollection = "friends")

    private fun observeIdsThenMapToProfiles(
        userId: String,
        subCollection: String
    ): Flow<List<User>> = callbackFlow {

        val listener = firestore.collection("users")
            .document(userId)
            .collection(subCollection)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val ids = snapshot?.documents?.map { it.id }.orEmpty()

                val cached = ids.mapNotNull { profileStore.getCached(it) }
                trySend(cached)

                launch(Dispatchers.IO) {
                    try {
                        profileStore.refreshMissing(ids)
                        val final = ids.mapNotNull { profileStore.getCached(it) }
                        trySend(final)
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        Log.e("FriendsRepository", "refreshMissing failed ($subCollection)", e)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    override fun searchUsers(query: String): Flow<List<User>> = flow {
        val snapshot = firestore.collection("users")
            .whereEqualTo("uniqueId", query)
            .get()
            .await()

        val users = snapshot.documents.mapNotNull { doc ->
            val user = doc.toObject(User::class.java) ?: return@mapNotNull null
            user.copy(userId = doc.id)
        }

        profileStore.putAll(users)
        emit(users)

    }.catch { e ->
        if (e is CancellationException) throw e

        Log.e("FriendsRepository", "searchUsers failed", e)
        emit(emptyList())
    }

    override fun sendFriendRequest(
        currentUserId: String,
        targetUser: User
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val sentDate = Timestamp.now()
            firestore.collection("users").document(targetUser.userId)
                .collection("requests").document(currentUserId)
                .set(mapOf("sentDate" to sentDate))
                .await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }

    override fun acceptFriendRequest(
        currentUserId: String,
        targetUser: User
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val date = Timestamp.now()
            val data = mapOf("sentDate" to date)

            firestore.collection("users").document(currentUserId)
                .collection("friends").document(targetUser.userId)
                .set(data).await()

            firestore.collection("users").document(targetUser.userId)
                .collection("friends").document(currentUserId)
                .set(data).await()

            firestore.collection("users").document(currentUserId)
                .collection("requests").document(targetUser.userId)
                .delete().await()

            profileStore.putAll(listOf(targetUser))

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }

    override fun deleteFriendRequest(
        currentUserId: String,
        targetUser: User
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firestore.collection("users").document(currentUserId)
                .collection("requests").document(targetUser.userId)
                .delete().await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }
}