package com.engyh.friendhub.data.repository

import androidx.core.net.toUri
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.Message
import com.engyh.friendhub.domain.repository.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ChatRepository {

    override fun getMessages(senderId: String, receiverId: String): Flow<List<Message>> = callbackFlow {
        val collection = firestore.collection("users")
            .document(senderId)
            .collection("friends")
            .document(receiverId)
            .collection("chats")
            .orderBy("time", Query.Direction.ASCENDING)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val messages = snapshot.toObjects(Message::class.java)
                trySend(messages)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(message: Message): Response<Boolean> {
        return try {

            firestore.collection("users")
                .document(message.senderId)
                .collection("friends")
                .document(message.receiverId)
                .collection("chats")
                .document(message.messageId)
                .set(message)
                .await()

            firestore.collection("users")
                .document(message.receiverId)
                .collection("friends")
                .document(message.senderId)
                .collection("chats")
                .document(message.messageId)
                .set(message)
                .await()

            Response.Success(true)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Oops, something went wrong.")
        }
    }

    override suspend fun uploadVoice(uriString: String): Response<String> {
        return try {
            val fileName = "voice_messages/${System.currentTimeMillis()}.3gp"
            val ref = storage.reference.child(fileName)
            ref.putFile(uriString.toUri()).await()
            val url = ref.downloadUrl.await().toString()
            Response.Success(url)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Oops, something went wrong.")
        }
    }

    override suspend fun setTyping(
        senderId: String,
        receiverId: String,
        isTyping: Boolean
    ) {
        try {
            val docRef = firestore.collection("users")
                .document(receiverId)
                .collection("friends")
                .document(senderId)

            val data = hashMapOf(
                "isTyping" to isTyping,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun observeTyping(
        senderId: String,
        receiverId: String
    ): Flow<Boolean> = callbackFlow {
        val docRef = firestore.collection("users")
            .document(receiverId)
            .collection("friends")
            .document(senderId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val isTyping = snapshot?.getBoolean("isTyping") ?: false
            trySend(isTyping)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun clearChatForUser(currentUserId: String, otherUserId: String): Response<Boolean> {
        return try {
            val chatsRef = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(otherUserId)
                .collection("chats")

            val snapshot = chatsRef.get().await()
            if (snapshot.isEmpty) return Response.Success(true)

            val batch = firestore.batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()

            firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(otherUserId)
                .set(
                    mapOf(
                        "lastMessage" to "Chat cleared",
                        "lastMessageDate" to Timestamp.now(),
                        "unreadMessages" to 0
                    ),
                    SetOptions.merge()
                )
                .await()

            Response.Success(true)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage ?: "Failed to clear chat")
        }
    }

    override suspend fun resetUnread(currentUserId: String, otherUserId: String) {
        try {
            firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(otherUserId)
                .set(mapOf("unreadMessages" to 0), SetOptions.merge())
                .await()
        } catch (_: Exception) {}
    }


    override suspend fun updateChatSummary(senderId: String, receiverId: String, lastMessage: String) {
        val now = Timestamp.now()

        val senderFriendDoc = firestore.collection("users")
            .document(senderId)
            .collection("friends")
            .document(receiverId)

        val receiverFriendDoc = firestore.collection("users")
            .document(receiverId)
            .collection("friends")
            .document(senderId)

        val batch = firestore.batch()

        batch.set(
            senderFriendDoc,
            mapOf(
                "lastMessage" to lastMessage,
                "lastMessageDate" to now,
                "unreadMessages" to 0
            ),
            SetOptions.merge()
        )

        batch.set(
            receiverFriendDoc,
            mapOf(
                "lastMessage" to lastMessage,
                "lastMessageDate" to now,
                "unreadMessages" to FieldValue.increment(1)
            ),
            SetOptions.merge()
        )

        batch.commit().await()
    }
}