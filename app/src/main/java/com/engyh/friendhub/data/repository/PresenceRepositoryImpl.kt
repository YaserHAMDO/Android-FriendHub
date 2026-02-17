package com.engyh.friendhub.data.repository

import com.engyh.friendhub.domain.repository.PresenceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PresenceRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase
) : PresenceRepository {

    private fun userRef() = auth.currentUser?.uid?.let { uid ->
        db.reference.child("users").child(uid)
    }

    override suspend fun startPresence() {
        val ref = userRef() ?: return

        ref.child("isOnline").onDisconnect().setValue(false).await()
        ref.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP).await()

        ref.child("isOnline").setValue(true).await()
    }

    override suspend fun setOnline() {
        val ref = userRef() ?: return
        ref.child("isOnline").setValue(true).await()
    }

    override suspend fun setOffline() {
        val ref = userRef() ?: return
        ref.child("isOnline").setValue(false).await()
        ref.child("lastSeen").setValue(ServerValue.TIMESTAMP).await()
    }

    override fun observeIsOnline(userId: String) = callbackFlow {
        val ref = db.getReference("users/$userId/isOnline")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Boolean::class.java) ?: false)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
