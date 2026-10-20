package com.shahab.fixadvisor.data.models

/**
 * A customer review of a shop. Stored in the `reviews` Firestore collection
 * (document id = review id, with a `shopId` field for filtering).
 *
 * The author's display name is denormalized here so review lists never need
 * to read private user documents.
 */
data class Review(
    val id: String = "",
    val shopId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val stars: Int = 5,
    /** e.g. "Screen replacement". From the fixed [RepairTypes] list. */
    val repairType: String = "",
    /** e.g. "iPhone 13". Free text. */
    val phoneModel: String = "",
    /** Amount paid in RM (optional). */
    val pricePaid: Double? = null,
    /** Free text, e.g. "1 hour", "2 days". */
    val timeTaken: String = "",
    /** "Original" / "OEM" / "Copy" / "Not sure". */
    val partsQuality: String = "",
    val wouldReturn: Boolean = true,
    val text: String = "",
    /** True when the review was written with the shop's verification code. */
    val verified: Boolean = false,
    /** Public reply from the shop owner (empty = no reply yet). */
    val ownerReply: String = "",
    val ownerReplyAt: Long? = null,
    val createdAt: Long = 0L
)
