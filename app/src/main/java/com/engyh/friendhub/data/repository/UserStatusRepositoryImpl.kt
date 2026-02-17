package com.engyh.friendhub.data.repository

import com.engyh.friendhub.domain.repository.UserStatusRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class UserStatusRepositoryImpl @Inject constructor(
    private val db: FirebaseDatabase
) : UserStatusRepository {

    override fun observeOnline(userId: String): Flow<Boolean> = callbackFlow {
        if (userId.isBlank()) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }

        val ref = db.reference.child("users").child(userId).child("isOnline")

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val online = snapshot.getValue(Boolean::class.java) ?: false
                trySend(online)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                trySend(false)
            }
        }

        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }
}
