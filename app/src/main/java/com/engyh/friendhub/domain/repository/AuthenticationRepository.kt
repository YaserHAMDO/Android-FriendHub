package com.engyh.friendhub.domain.repository

import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {

    suspend fun signIn(email: String, password: String): Flow<Response<AuthResult>>

    suspend fun signUp(email: String, password: String): Flow<Response<AuthResult>>

    suspend fun signUp2(
        email: String,
        password: String,
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ): Flow<Response<AuthResult>>

    suspend fun signUp3(
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ): Flow<Response<AuthResult>>

    suspend fun resetPassword(email: String): Flow<Response<Void?>>

    suspend fun logout()

    suspend fun userUid(): String

    suspend fun isLoggedIn(): Boolean

    suspend fun getUserProfile(uid: String): Flow<Response<User>>

    suspend fun checkIfUserExists(uid: String): Boolean

    suspend fun signInWithCredential(credential: AuthCredential): Flow<Response<AuthResult>>

    suspend fun deleteAccount(): Flow<Response<Void?>>
}