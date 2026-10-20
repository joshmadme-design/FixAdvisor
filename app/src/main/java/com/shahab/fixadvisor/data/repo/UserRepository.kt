package com.shahab.fixadvisor.data.repo

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shahab.fixadvisor.data.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Private per-user document in the `users` collection (document id = uid).
 * Holds favourites and the owned shop id. Only the owner can read/write
 * (see README security rules); the public never reads these documents.
 */
class UserRepository(context: Context) {

    private val db: FirebaseFirestore = Firebase.firestore
    private val users = db.collection("users")

    /** Live profile for the given uid (null until created). */
    fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(mapUserProfile(uid, snapshot?.data))
        }
        awaitClose { registration.remove() }
    }

    /** Creates the profile on first sign-in; updates name/email on later sign-ins. */
    suspend fun ensureProfile(user: FirebaseUser) {
        val ref = users.document(user.uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(
                userProfileToMap(
                    UserProfile(
                        uid = user.uid,
                        displayName = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "displayName" to (user.displayName.orEmpty()),
                    "email" to (user.email.orEmpty())
                )
            ).await()
        }
    }

    suspend fun setOwnedShop(uid: String, shopId: String) {
        users.document(uid).update("ownedShopId", shopId).await()
    }

    suspend fun addFavorite(uid: String, shopId: String) {
        val ref = users.document(uid)
        val current = ref.get().await().data
            ?.let { (it["favoriteShopIds"] as? List<*>)?.filterIsInstance<String>() }
            .orEmpty()
        if (shopId !in current) {
            ref.update("favoriteShopIds", current + shopId).await()
        }
    }

    suspend fun removeFavorite(uid: String, shopId: String) {
        val ref = users.document(uid)
        val current = ref.get().await().data
            ?.let { (it["favoriteShopIds"] as? List<*>)?.filterIsInstance<String>() }
            .orEmpty()
        ref.update("favoriteShopIds", current - shopId).await()
    }
}

/** Manual Firestore -> UserProfile mapping with safe casts. */
fun mapUserProfile(uid: String, data: Map<String, Any?>?): UserProfile? {
    if (data == null) return null
    fun str(key: String): String = (data[key] as? String).orEmpty()
    return UserProfile(
        uid = uid,
        displayName = str("displayName"),
        email = str("email"),
        favoriteShopIds = (data["favoriteShopIds"] as? List<*>)
            ?.filterIsInstance<String>().orEmpty(),
        ownedShopId = data["ownedShopId"] as? String,
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
    )
}

/** UserProfile -> Firestore map. */
fun userProfileToMap(profile: UserProfile): Map<String, Any?> = mapOf(
    "displayName" to profile.displayName,
    "email" to profile.email,
    "favoriteShopIds" to profile.favoriteShopIds,
    "ownedShopId" to profile.ownedShopId,
    "createdAt" to profile.createdAt
)
