package com.engyh.friendhub.data.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.data.cache.UserProfileStore
import com.engyh.friendhub.domain.model.Comment
import com.engyh.friendhub.domain.model.Post
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.repository.PostsRepository
import com.engyh.friendhub.domain.repository.StorageRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class PostsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthenticationRepository,
    private val profileStore: UserProfileStore
) : PostsRepository {

    override fun getPosts(): Flow<Response<List<Post>>> = callbackFlow {
        val myId = authRepository.userUid()
        if (myId.isBlank()) {
            trySend(Response.Error("User not logged in"))
            close()
            return@callbackFlow
        }

        trySend(Response.Loading)

        val listeners = mutableListOf<ListenerRegistration>()
        val postsMap = ConcurrentHashMap<String, Post>()

        val likeListeners = ConcurrentHashMap<String, ListenerRegistration>()
        val commentCountListeners = ConcurrentHashMap<String, ListenerRegistration>()

        fun key(ownerId: String, postId: String) = "${ownerId}_$postId"

        fun enrich(post: Post): Post {
            val cached = profileStore.getCached(post.userId)
            return if (cached != null) {
                post.copy(
                    userName = cached.name,
                    userImageUrl = cached.imageUrl
                )
            } else post
        }

        fun emit() {
            val list = postsMap.values
                .map(::enrich)
                .sortedByDescending { it.timestamp }
            trySend(Response.Success(list))
        }

        suspend fun loadFriendIdsOnce(): List<String> {
            val snap = firestore.collection("users")
                .document(myId)
                .collection("friends")
                .get()
                .await()
            return snap.documents.map { it.id } + myId
        }

        try {
            val owners = loadFriendIdsOnce()

            owners.forEach { ownerId ->
                val postsListener = firestore.collection("users")
                    .document(ownerId)
                    .collection("posts")
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(Response.Error(err.message ?: "Posts listener error"))
                            return@addSnapshotListener
                        }
                        if (snap == null) return@addSnapshotListener

                        for (change in snap.documentChanges) {
                            val doc = change.document
                            val base = doc.toObject(Post::class.java)
                            val postId = if (base.postId.isNotBlank()) base.postId else doc.id
                            val mapKey = key(ownerId, postId)

                            when (change.type) {
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    postsMap.remove(mapKey)
                                    likeListeners.remove(mapKey)?.remove()
                                    commentCountListeners.remove(mapKey)?.remove()
                                }

                                else -> {
                                    val post = base.copy(
                                        postId = postId,
                                        userId = ownerId
                                    )
                                    postsMap[mapKey] = post

                                    if (!likeListeners.containsKey(mapKey)) {
                                        val l = doc.reference.collection("likes")
                                            .addSnapshotListener { likesSnap, _ ->
                                                val current = postsMap[mapKey] ?: return@addSnapshotListener
                                                postsMap[mapKey] = current.copy(
                                                    likesCount = likesSnap?.size() ?: 0,
                                                    isLikedByMe = likesSnap?.documents?.any { it.id == myId } == true
                                                )
                                                emit()
                                            }
                                        likeListeners[mapKey] = l
                                        listeners.add(l)
                                    }

                                    if (!commentCountListeners.containsKey(mapKey)) {
                                        val c = doc.reference.collection("comments")
                                            .addSnapshotListener { commentsSnap, _ ->
                                                val current = postsMap[mapKey] ?: return@addSnapshotListener
                                                postsMap[mapKey] = current.copy(
                                                    commentCount = commentsSnap?.size() ?: 0
                                                )
                                                emit()
                                            }
                                        commentCountListeners[mapKey] = c
                                        listeners.add(c)
                                    }
                                }
                            }
                        }

                        emit()
                    }

                listeners.add(postsListener)
            }
        } catch (e: Exception) {
            trySend(Response.Error(e.message ?: "Failed to load posts"))
        }

        awaitClose {
            listeners.forEach { it.remove() }
            likeListeners.values.forEach { it.remove() }
            commentCountListeners.values.forEach { it.remove() }
            postsMap.clear()
        }
    }

    override fun getUserPosts(userId: String): Flow<Response<List<Post>>> = callbackFlow {
        val myId = authRepository.userUid()
        if (myId.isBlank()) {
            trySend(Response.Error("User not logged in"))
            close()
            return@callbackFlow
        }

        trySend(Response.Loading)

        val listeners = mutableListOf<ListenerRegistration>()
        val postsMap = ConcurrentHashMap<String, Post>()

        val likeListeners = ConcurrentHashMap<String, ListenerRegistration>()
        val commentCountListeners = ConcurrentHashMap<String, ListenerRegistration>()

        fun key(ownerId: String, postId: String) = "${ownerId}_$postId"

        fun enrich(post: Post): Post {
            val cached = profileStore.getCached(post.userId)
            return if (cached != null) {
                post.copy(
                    userName = cached.name,
                    userImageUrl = cached.imageUrl
                )
            } else post
        }

        fun emit() {
            val list = postsMap.values
                .map(::enrich)
                .sortedByDescending { it.timestamp }
            trySend(Response.Success(list))
        }
        try {

            val postsListener = firestore.collection("users")
                .document(userId)
                .collection("posts")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        trySend(Response.Error(err.message ?: "Posts listener error"))
                        return@addSnapshotListener
                    }
                    if (snap == null) return@addSnapshotListener

                    for (change in snap.documentChanges) {
                        val doc = change.document
                        val base = doc.toObject(Post::class.java)
                        val postId = if (base.postId.isNotBlank()) base.postId else doc.id
                        val mapKey = key(userId, postId)

                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                postsMap.remove(mapKey)
                                likeListeners.remove(mapKey)?.remove()
                                commentCountListeners.remove(mapKey)?.remove()
                            }

                            else -> {
                                val post = base.copy(
                                    postId = postId,
                                    userId = userId
                                )
                                postsMap[mapKey] = post

                                if (!likeListeners.containsKey(mapKey)) {
                                    val l = doc.reference.collection("likes")
                                        .addSnapshotListener { likesSnap, _ ->
                                            val current =
                                                postsMap[mapKey] ?: return@addSnapshotListener
                                            postsMap[mapKey] = current.copy(
                                                likesCount = likesSnap?.size() ?: 0,
                                                isLikedByMe = likesSnap?.documents?.any { it.id == myId } == true
                                            )
                                            emit()
                                        }
                                    likeListeners[mapKey] = l
                                    listeners.add(l)
                                }

                                if (!commentCountListeners.containsKey(mapKey)) {
                                    val c = doc.reference.collection("comments")
                                        .addSnapshotListener { commentsSnap, _ ->
                                            val current =
                                                postsMap[mapKey] ?: return@addSnapshotListener
                                            postsMap[mapKey] = current.copy(
                                                commentCount = commentsSnap?.size() ?: 0
                                            )
                                            emit()
                                        }
                                    commentCountListeners[mapKey] = c
                                    listeners.add(c)
                                }
                            }
                        }
                    }

                    emit()
                }

            listeners.add(postsListener)

        }

        catch (e: Exception) {
            trySend(Response.Error(e.message ?: "Failed to load posts"))
        }

        awaitClose {
            listeners.forEach { it.remove() }
            likeListeners.values.forEach { it.remove() }
            commentCountListeners.values.forEach { it.remove() }
            postsMap.clear()
        }
    }
    override suspend fun createPost(text: String, imageUri: String?): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val myId = authRepository.userUid()
            if (myId.isBlank()) {
                emit(Response.Error("User not logged in"))
                return@flow
            }

            val cached = profileStore.getCached(myId)
            val myName = cached?.name.orEmpty()
            val myImage = cached?.imageUrl.orEmpty()

            var imageUrl = ""
            if (imageUri != null) {
                val uuid = UUID.randomUUID().toString()
                val path = "post_images/$uuid"

                storageRepository.uploadImage(imageUri, path).collect { upload ->
                    when (upload) {
                        is Response.Loading -> Unit
                        is Response.Success -> imageUrl = upload.data
                        is Response.Error -> {
                            emit(Response.Error(upload.message))
                            return@collect
                        }
                    }
                }
            }

            val postId = firestore.collection("users")
                .document(myId)
                .collection("posts")
                .document().id

            val post = Post(
                postId = postId,
                userId = myId,
                userName = myName,
                userImageUrl = myImage,
                text = text,
                imageUrl = imageUrl,
                timestamp = Timestamp.now()
            )

            firestore.collection("users")
                .document(myId)
                .collection("posts")
                .document(postId)
                .set(post)
                .await()

            emit(Response.Success(true))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to create post"))
        }
    }

    override suspend fun likePost(
        postId: String,
        postOwnerId: String,
        liked: Boolean
    ): Flow<Response<Boolean>> = flow {
        try {
            val myId = authRepository.userUid()
            if (myId.isBlank()) {
                emit(Response.Error("User not logged in"))
                return@flow
            }

            val ref = firestore.collection("users")
                .document(postOwnerId)
                .collection("posts")
                .document(postId)
                .collection("likes")
                .document(myId)

            if (liked) ref.set(mapOf("likedAt" to Timestamp.now())).await()
            else ref.delete().await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Like failed"))
        }
    }

    override fun getComments(
        postId: String,
        postOwnerId: String
    ): Flow<Response<List<Comment>>> = callbackFlow {

        trySend(Response.Loading)

        val ref = firestore.collection("users")
            .document(postOwnerId)
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = ref.addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(Response.Error(e.message ?: "Failed to listen for comments"))
                return@addSnapshotListener
            }

            val enriched = snap?.documents.orEmpty().mapNotNull { doc ->
                val commentId = doc.getString("commentId") ?: doc.id
                val userId = doc.getString("userId") ?: return@mapNotNull null
                val commentText = doc.getString("commentText") ?: ""

                val ts = doc.getTimestamp("timestamp") ?: Timestamp.now()

                val cached = profileStore.getCached(userId)
                val userName = cached?.name ?: (doc.getString("userName") ?: "")
                val userImage = cached?.imageUrl ?: (doc.getString("userImage") ?: "")

                Comment(
                    commentId = commentId,
                    userId = userId,
                    userImage = userImage,
                    userName = userName,
                    commentText = commentText,
                    timestamp = ts
                )
            }

            trySend(Response.Success(enriched))
        }

        awaitClose { listener.remove() }
    }

    override suspend fun addComment(
        postId: String,
        postOwnerId: String,
        commentText: String
    ): Flow<Response<Boolean>> = flow {

        emit(Response.Loading)
        try {
            val myId = authRepository.userUid()
            if (myId.isBlank()) {
                emit(Response.Error("User not logged in"))
                return@flow
            }

            val cached = profileStore.getCached(myId)
            val myName = cached?.name ?: ""
            val myImage = cached?.imageUrl ?: ""

            val commentRef = firestore.collection("users")
                .document(postOwnerId)
                .collection("posts")
                .document(postId)
                .collection("comments")
                .document()

            val data = mapOf(
                "commentId" to commentRef.id,
                "userId" to myId,
                "userName" to myName,
                "userImage" to myImage,
                "commentText" to commentText,
                "timestamp" to FieldValue.serverTimestamp()
            )

            commentRef.set(data).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to add comment"))
        }
    }
}