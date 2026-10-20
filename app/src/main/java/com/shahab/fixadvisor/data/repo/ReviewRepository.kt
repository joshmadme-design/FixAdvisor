package com.shahab.fixadvisor.data.repo

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shahab.fixadvisor.data.models.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore `reviews` collection.
 *
 * Review writes/deletes update the parent shop's denormalized
 * `averageRating`/`reviewCount` inside a transaction, so the shop list
 * never needs to aggregate on the client. Reviews are queried with a
 * single-field filter (`shopId` or `authorUid`) and sorted client-side —
 * deliberate for Phase 1 so no composite Firestore indexes need creating
 * (see README).
 */
class ReviewRepository(context: Context) {

    private val db: FirebaseFirestore = Firebase.firestore
    private val reviews = db.collection("reviews")
    private val shops = db.collection("shops")

    /** Live reviews for one shop, newest first. */
    fun observeReviewsForShop(shopId: String): Flow<List<Review>> = callbackFlow {
        val registration = reviews
            .whereEqualTo("shopId", shopId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { mapReview(it.id, it.data) }
                    .orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Live reviews written by one user, newest first. */
    fun observeMyReviews(uid: String): Flow<List<Review>> = callbackFlow {
        val registration = reviews
            .whereEqualTo("authorUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { mapReview(it.id, it.data) }
                    .orEmpty()
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Writes a review and updates the shop's aggregates in one transaction.
     * The caller must ensure the user is signed in and has not already
     * reviewed this shop (see WriteReviewViewModel).
     */
    suspend fun writeReview(review: Review): Result<Unit> = runCatching {
        require(review.authorUid.isNotBlank()) { "Sign-in required to write a review." }
        require(review.stars in 1..5) { "Star rating must be 1–5." }
        val now = System.currentTimeMillis()
        val withTimestamps = review.copy(createdAt = now)
        db.runTransaction { tx ->
            val shopRef = shops.document(withTimestamps.shopId)
            val shopSnap = tx.get(shopRef)
            val shop = mapShop(shopSnap.id, shopSnap.data)
                ?: throw IllegalStateException("Shop no longer exists.")
            val reviewRef = reviews.document()
            tx.set(reviewRef, reviewToMap(withTimestamps))
            val newCount = shop.reviewCount + 1
            val newAverage = (shop.averageRating * shop.reviewCount + withTimestamps.stars) / newCount
            tx.update(
                shopRef, mapOf(
                    "averageRating" to newAverage,
                    "reviewCount" to newCount,
                    "updatedAt" to now
                )
            )
        }.await()
    }

    /**
     * The shop owner replies to a review. Only updates the reply fields —
     * the author and content stay untouched (rules also enforce this).
     */
    suspend fun replyToReview(reviewId: String, reply: String): Result<Unit> = runCatching {
        require(reply.isNotBlank()) { "Reply cannot be empty." }
        reviews.document(reviewId).update(
            mapOf(
                "ownerReply" to reply.trim(),
                "ownerReplyAt" to System.currentTimeMillis()
            )
        ).await()
    }

    /** The author deletes their own review; shop aggregates are adjusted. */
    suspend fun deleteReview(review: Review): Result<Unit> = runCatching {
        db.runTransaction { tx ->
            val shopRef = shops.document(review.shopId)
            val shopSnap = tx.get(shopRef)
            val shop = mapShop(shopSnap.id, shopSnap.data)
                ?: throw IllegalStateException("Shop no longer exists.")
            tx.delete(reviews.document(review.id))
            val newCount = (shop.reviewCount - 1).coerceAtLeast(0)
            val newAverage = if (newCount == 0) {
                0.0
            } else {
                ((shop.averageRating * shop.reviewCount) - review.stars)
                    .coerceAtLeast(0.0) / newCount
            }
            tx.update(
                shopRef, mapOf(
                    "averageRating" to newAverage,
                    "reviewCount" to newCount,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }.await()
    }
}

/** Manual Firestore -> Review mapping with safe casts. */
fun mapReview(id: String, data: Map<String, Any?>?): Review? {
    if (data == null) return null
    fun str(key: String): String = (data[key] as? String).orEmpty()
    fun long(key: String): Long = (data[key] as? Number)?.toLong() ?: 0L
    fun optLong(key: String): Long? = (data[key] as? Number)?.toLong()
    return Review(
        id = id,
        shopId = str("shopId"),
        authorUid = str("authorUid"),
        authorName = str("authorName"),
        stars = (data["stars"] as? Number)?.toInt()?.coerceIn(1, 5) ?: 5,
        repairType = str("repairType"),
        phoneModel = str("phoneModel"),
        pricePaid = (data["pricePaid"] as? Number)?.toDouble(),
        timeTaken = str("timeTaken"),
        partsQuality = str("partsQuality"),
        wouldReturn = (data["wouldReturn"] as? Boolean) ?: true,
        text = str("text"),
        verified = (data["verified"] as? Boolean) ?: false,
        ownerReply = str("ownerReply"),
        ownerReplyAt = optLong("ownerReplyAt"),
        createdAt = long("createdAt")
    )
}

/** Review -> Firestore map. */
fun reviewToMap(review: Review): Map<String, Any?> = mapOf(
    "shopId" to review.shopId,
    "authorUid" to review.authorUid,
    "authorName" to review.authorName,
    "stars" to review.stars,
    "repairType" to review.repairType,
    "phoneModel" to review.phoneModel,
    "pricePaid" to review.pricePaid,
    "timeTaken" to review.timeTaken,
    "partsQuality" to review.partsQuality,
    "wouldReturn" to review.wouldReturn,
    "text" to review.text,
    "verified" to review.verified,
    "ownerReply" to review.ownerReply,
    "ownerReplyAt" to review.ownerReplyAt,
    "createdAt" to review.createdAt
)
