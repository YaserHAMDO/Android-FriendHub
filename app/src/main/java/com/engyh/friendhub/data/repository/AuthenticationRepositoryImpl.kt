package com.engyh.friendhub.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.random.Random

class AuthenticationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : AuthenticationRepository {

    override suspend fun userUid(): String = auth.currentUser?.uid ?: ""

    override suspend fun isLoggedIn(): Boolean = auth.currentUser != null

    override suspend fun logout() = auth.signOut()

    override suspend fun signIn(email: String, password: String): Flow<Response<AuthResult>> = flow {
        try {
            emit(Response.Loading)
            val data = auth.signInWithEmailAndPassword(email, password).await()
            emit(Response.Success(data))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }

    override suspend fun signUp(email: String, password: String): Flow<Response<AuthResult>> =
        flow {
            try {
                emit(Response.Loading)
                val data = auth.createUserWithEmailAndPassword(email, password).await()
                emit(Response.Success(data))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
            }
        }

    override suspend fun resetPassword(email: String): Flow<Response<Void?>> = flow {
        try {
            emit(Response.Loading)
            val data = auth.sendPasswordResetEmail(email).await()
            emit(Response.Success(data))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }


    override suspend fun signUp2(
        email: String,
        password: String,
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ): Flow<Response<AuthResult>> = flow {
        var createdUid: String? = null
        try {
            emit(Response.Loading)

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser =
                authResult.user ?: throw Exception("User creation succeeded but user is null")
            createdUid = firebaseUser.uid

            val imageUrl = uploadProfileImage(createdUid, image)

            val uniqueId = generateUniqueId()

            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                Log.w("AuthRepo", "Fetching FCM registration token failed", e)
                ""
            }

            val profile = User(
                userId = createdUid,
                email = email,
                fcm = fcmToken,
                name = userName,
                uniqueId = uniqueId,
                about = about,
                birthdate = birthdate,
                gender = gender,
                lat = location.first,
                lon = location.second,
                imageUrl = imageUrl,
                imageName = ""
            )

            firestore.collection("users").document(createdUid).set(profile).await()

            emit(Response.Success(authResult))
        } catch (e: Exception) {
            try {
                val current = auth.currentUser
                if (createdUid != null && current != null && current.uid == createdUid) {
                    current.delete().await()
                }
            } catch (_: Exception) {
            }
            emit(Response.Error(mapFirebaseException(e)))
        }
    }

    override suspend fun signUp3(
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ): Flow<Response<AuthResult>> = flow {
        try {
            emit(Response.Loading)

            val firebaseUser = auth.currentUser
                ?: throw Exception("No authenticated user found. Please sign in (e.g. via Google) first.")
            val createdUid = firebaseUser.uid

            val email = firebaseUser.email ?: ""
            val imageUrl = uploadProfileImage(createdUid, image)
            val uniqueId = generateUniqueId()
            val fcmToken = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                Log.w("AuthRepo", "Fetching FCM registration token failed", e)
                ""
            }

            val profile = User(
                userId = createdUid,
                email = email,
                fcm = fcmToken,
                name = userName,
                uniqueId = uniqueId,
                about = about,
                birthdate = birthdate,
                gender = gender,
                lat = location.first,
                lon = location.second,
                imageUrl = imageUrl,
                imageName = ""
            )

            firestore.collection("users").document(createdUid).set(profile).await()

            @Suppress("UNCHECKED_CAST")
            emit(Response.Success(null) as Response<AuthResult>)

        } catch (e: Exception) {
            emit(Response.Error(mapFirebaseException(e)))
        }
    }


    override suspend fun deleteAccount(): Flow<Response<Void?>> = flow {
        try {
            emit(Response.Loading)
            val user = auth.currentUser
            if (user == null) {
                Log.e("AuthRepo", "deleteAccount: No user found")
                emit(Response.Error("No user logged in"))
                return@flow
            }
            val uid = user.uid
            Log.d("AuthRepo", "deleteAccount: Starting deletion for user $uid")

            try {
                firestore.collection("users").document(uid).delete().await()
                Log.d("AuthRepo", "deleteAccount: Firestore document deleted")
            } catch (e: Exception) {
                Log.e("AuthRepo", "deleteAccount: Failed to delete Firestore document", e)
            }

            try {
                storage.reference.child("profile_images/$uid.jpg").delete().await()
                Log.d("AuthRepo", "deleteAccount: Storage imageUrl deleted")
            } catch (e: Exception) {
                Log.e("AuthRepo", "deleteAccount: Failed to delete Storage imageUrl (might not exist)", e)
            }

            user.delete().await()
            Log.d("AuthRepo", "deleteAccount: Auth user deleted")

            emit(Response.Success(null))
        } catch (e: Exception) {
            Log.e("AuthRepo", "deleteAccount: Exception caught", e)
            emit(Response.Error(mapFirebaseException(e)))
        }
    }

    private suspend fun generateUniqueId(): String {
        var uniqueId: String
        while (true) {
            uniqueId = Random.nextInt(100_000, 1_000_000).toString()
            if (!isIdExists(uniqueId)) {
                break
            }
        }
        return uniqueId
    }

    private suspend fun isIdExists(id: String): Boolean {
        return try {
            val documents = firestore.collection("users")
                .whereEqualTo("uniqueId", id)
                .limit(1)
                .get()
                .await()
            !documents.isEmpty
        } catch (e: Exception) {
            Log.e("AuthRepo", "isIdExists check failed", e)
            false
        }
    }


    private suspend fun uploadProfileImage(uid: String, imageUri: String): String {
        val uri = Uri.parse(imageUri)

        val bytes = uriToJpegBytes(
            resolver = context.contentResolver,
            uri = uri,
            maxSize = 1080,
            quality = 85
        )

        val storageRef = storage.reference.child("profile_images/$uid")
        val uploadTask = storageRef.putBytes(bytes).await()
        return uploadTask.storage.downloadUrl.await().toString()
    }

    private suspend fun uriToJpegBytes(
        resolver: android.content.ContentResolver,
        uri: Uri,
        maxSize: Int,
        quality: Int
    ): ByteArray = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        resolver.openInputStream(uri).use { input ->
            val original = BitmapFactory.decodeStream(input)
                ?: throw IllegalArgumentException("Failed to decode image")
            val scaled = original.scaleDown(maxSize)

            java.io.ByteArrayOutputStream().use { baos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                baos.toByteArray()
            }
        }
    }

    private fun Bitmap.scaleDown(maxSize: Int): Bitmap {
        val w = width
        val h = height
        if (w <= maxSize && h <= maxSize) return this

        val ratio = if (w > h) maxSize.toFloat() / w else maxSize.toFloat() / h
        val newW = (w * ratio).toInt()
        val newH = (h * ratio).toInt()
        return Bitmap.createScaledBitmap(this, newW, newH, true)
    }


    private fun mapFirebaseException(e: Exception): String {
        return when (e) {
            is FirebaseAuthUserCollisionException -> "This email is already in use."
            is FirebaseAuthWeakPasswordException -> "Weak password (should be at least 6 characters)."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or credentials."
            is FirebaseAuthRecentLoginRequiredException -> "For security, please log out and sign in again before deleting your account."
            is FirebaseFirestoreException -> "Database error: ${e.message}"
            else -> e.localizedMessage ?: "Unknown error"
        }
    }

    override suspend fun getUserProfile(uid: String): Flow<Response<User>> = flow {
        try {
            emit(Response.Loading)
            val data = firestore.collection("users").document(uid).get().await()
                .toObject(User::class.java)
            emit(Response.Success(data ?: User()))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Oops, something went wrong."))
        }
    }

    override suspend fun checkIfUserExists(uid: String): Boolean {
        val doc = firestore.collection("users").document(uid).get().await()
        return doc.exists()
    }

    override suspend fun signInWithCredential(credential: AuthCredential): Flow<Response<AuthResult>> = flow {
        try {
            emit(Response.Loading)
            val data = auth.signInWithCredential(credential).await()
            emit(Response.Success(data))
        } catch (e: Exception) {
            emit(Response.Error(mapFirebaseException(e)))
        }
    }

}
