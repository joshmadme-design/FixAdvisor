package com.shahab.fixadvisor.data.models

/**
 * Private per-user document in the `users` collection (document id = uid).
 * Only the owner can read/write it (see README security rules).
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    /** Shop ids the user favourited. */
    val favoriteShopIds: List<String> = emptyList(),
    /** Shop id this user registered as owner (Phase 1: one shop per user). */
    val ownedShopId: String? = null,
    val createdAt: Long = 0L
)
