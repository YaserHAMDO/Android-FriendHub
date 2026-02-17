package com.engyh.friendhub.data.repository

import android.util.Log
import com.engyh.friendhub.data.cache.UserProfileStore
import com.engyh.friendhub.domain.model.Chat
import com.engyh.friendhub.domain.repository.ChatListRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatListRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileStore: UserProfileStore
) : ChatListRepository {

    override fun getChatList(userId: String): Flow<List<Chat>> = callbackFlow {

        val listener = firestore.collection("users")
            .document(userId)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendIds = ArrayList<String>(snapshot.size())
                val metaMap = HashMap<String, ChatInfoMeta>(snapshot.size())

                for (doc in snapshot.documents) {
                    val lastMessage = doc.getString("lastMessage").orEmpty()
                    if (lastMessage.isBlank()) continue

                    val friendId = doc.id
                    val unreadMessages = doc.getLong("unreadMessages") ?: 0L
                    val lastMessageDate = doc.getTimestamp("lastMessageDate") ?: Timestamp.now()

                    friendIds.add(friendId)

                    metaMap[friendId] = ChatInfoMeta(
                        lastMessage = lastMessage,
                        lastMessageDate = lastMessageDate,
                        unreadMessages = unreadMessages
                    )
                }

                if (friendIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val cachedData = friendIds.mapNotNull { friendId ->
                    val fetchedProfile = profileStore.getCached(friendId) ?: return@mapNotNull null
                    val mMap = metaMap[friendId] ?: return@mapNotNull null
                    Chat(
                        userId = friendId,
                        name = fetchedProfile.name,
                        imageUrl = fetchedProfile.imageUrl,
                        lastMessage = mMap.lastMessage,
                        lastMessageDate = mMap.lastMessageDate,
                        unreadMessages = mMap.unreadMessages
                    )
                }.sortedByDescending { it.lastMessageDate }

                if (cachedData.isNotEmpty()) trySend(cachedData)

                launch(Dispatchers.IO) {
                    try {
                        profileStore.refreshMissing(friendIds)

                        val final = friendIds.mapNotNull { friendId ->
                            val fetchedProfile = profileStore.getCached(friendId) ?: return@mapNotNull null
                            val m = metaMap[friendId] ?: return@mapNotNull null
                            Chat(
                                userId = friendId,
                                name = fetchedProfile.name,
                                imageUrl = fetchedProfile.imageUrl,
                                lastMessage = m.lastMessage,
                                lastMessageDate = m.lastMessageDate,
                                unreadMessages = m.unreadMessages
                            )
                        }.sortedByDescending { it.lastMessageDate }

                        trySend(final)
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        Log.e("ChatListRepository", "refreshMissing failed", e)
                    }
                }
            }

        awaitClose { listener.remove() }
    }.distinctUntilChanged()

    private data class ChatInfoMeta(
        val lastMessage: String,
        val lastMessageDate: Timestamp,
        val unreadMessages: Long
    )
}